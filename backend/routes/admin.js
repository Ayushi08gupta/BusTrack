const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const User = require('../models/User');
const Bus = require('../models/Bus');
const Route = require('../models/Route');
const { protect, adminOnly } = require('../middleware/authMiddleware');
const { sendCredentialsEmail } = require('../utils/email');

// Helper to generate credentials
const generateTempCredentials = (name) => {
  const cleanName = name.toLowerCase().replace(/\s+/g, '');
  const randomNum = Math.floor(1000 + Math.random() * 9000);
  const username = `${cleanName}${randomNum}`;
  const tempPassword = Math.random().toString(36).slice(-8);
  return { username, tempPassword };
};

// @desc    Get all users (Matching Android User model)
router.get('/users', protect, adminOnly, async (req, res) => {
  try {
    const users = await User.find().select('-password');
    res.json(users);
  } catch (error) {
    res.status(500).json({ message: 'Server error loading users' });
  }
});

// @desc    Create User & handle Email resilience
router.post('/users', protect, adminOnly, async (req, res) => {
  const { name, email, role, assignedBusId } = req.body;
  if (!email || !role) return res.status(400).json({ message: 'Email and Role are required' });

  try {
    const userExists = await User.findOne({ email });
    if (userExists) return res.status(400).json({ message: 'User already exists' });

    const { username, tempPassword } = generateTempCredentials(name || email.split('@')[0]);
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(tempPassword, salt);

    const user = await User.create({
      name: name || email.split('@')[0],
      email,
      username,
      password: hashedPassword,
      role: role.toLowerCase(),
      isFirstLogin: true,
      assignedBusId: assignedBusId || null
    });

    // Attempt to send email, but don't fail if it crashes (EAUTH fix)
    const emailSent = await sendCredentialsEmail(email, user.name, username, tempPassword);
    
    if (emailSent) {
      res.status(201).json({ message: 'User created & credentials emailed!', user });
    } else {
      // If EAUTH happens, provide the password to the Admin manually
      res.status(201).json({ 
        message: `User created! (Email failed to send. Temp Password: ${tempPassword})`, 
        user 
      });
    }
  } catch (error) {
    console.error('User creation error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
});

// @desc    Add stop to a bus's route
// @route   POST /api/admin/bus/:busId/stops
router.post('/bus/:busId/stops', protect, adminOnly, async (req, res) => {
  const { name, latitude, longitude } = req.body;
  try {
    const bus = await Bus.findById(req.params.busId);
    if (!bus) return res.status(404).json({ message: 'Bus not found' });
    if (!bus.routeId) return res.status(400).json({ message: 'This bus has no route assigned. Assign a route first.' });

    const route = await Route.findById(bus.routeId);
    if (!route) return res.status(404).json({ message: 'Route not found' });

    route.stops.push({ name, latitude, longitude });
    await route.save();
    res.json({ message: 'Stop added successfully to ' + bus.busNumber, route });
  } catch (error) {
    console.error('Add stop error:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Add stop (Existing endpoint maintained for compatibility)
router.post('/routes/:routeId/stops', protect, adminOnly, async (req, res) => {
  const { name, latitude, longitude } = req.body;
  try {
    const route = await Route.findById(req.params.routeId);
    if (!route) return res.status(404).json({ message: 'Route not found' });
    route.stops.push({ name, latitude, longitude });
    await route.save();
    res.json({ message: 'Stop added successfully', route });
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

router.patch('/users/:id/status', protect, adminOnly, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ message: 'User not found' });
    user.isActive = req.body.isActive !== undefined ? req.body.isActive : !user.isActive;
    await user.save();
    res.json({ message: 'Status updated' });
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

router.delete('/users/:id', protect, adminOnly, async (req, res) => {
  try {
    await User.findByIdAndDelete(req.params.id);
    res.json({ message: 'User deleted' });
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Assign driver and route to bus
// @route   POST /api/admin/assign
router.post('/assign', protect, adminOnly, async (req, res) => {
  const { driverId, busId, routeId } = req.body;
  console.log('Assignment request:', { driverId, busId, routeId });
  try {
    const bus = await Bus.findById(busId);
    if (!bus) {
        console.log('Bus not found:', busId);
        return res.status(404).json({ message: 'Bus not found' });
    }

    if (driverId) {
        try {
            const driver = await User.findById(driverId);
            if (!driver || driver.role !== 'driver') {
                console.log('Driver not found or not a driver:', driverId);
                return res.status(404).json({ message: 'Driver not found' });
            }
            bus.driverId = driverId;
            driver.assignedBusId = busId;
            await driver.save();
        } catch (err) {
            console.error('Error finding/saving driver:', err.message);
            return res.status(400).json({ message: 'Invalid Driver ID' });
        }
    }

    if (routeId) {
        try {
            const route = await Route.findById(routeId);
            if (!route) {
                console.log('Route not found:', routeId);
                return res.status(404).json({ message: 'Route not found' });
            }
            bus.routeId = routeId;
        } catch (err) {
            console.error('Error finding route:', err.message);
            return res.status(400).json({ message: 'Invalid Route ID' });
        }
    }

    await bus.save();
    console.log('Assignment successful for bus:', busId);
    res.json({ message: 'Assignment successful' });
  } catch (error) {
    console.error('Assignment server error:', error.message);
    res.status(500).json({ message: 'Server error: ' + error.message });
  }
});

module.exports = router;
