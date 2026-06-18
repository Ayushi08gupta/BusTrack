const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const User = require('../models/User');
const Bus = require('../models/Bus');
const Route = require('../models/Route');
const Complaint = require('../models/Complaint');
const { protect, adminOnly } = require('../middleware/authMiddleware');
const { sendCredentialsEmail } = require('../utils/email');

// Helper to generate credentials
const generateTempCredentials = (name) => {
  const cleanName = (name || 'user').toLowerCase().replace(/\s+/g, '');
  const randomNum = Math.floor(1000 + Math.random() * 9000);
  const username = `${cleanName}${randomNum}`;
  const tempPassword = Math.random().toString(36).slice(-8);
  return { username, tempPassword };
};

// @desc    Get all users
router.get('/users', protect, adminOnly, async (req, res) => {
  try {
    const users = await User.find().select('-password');
    res.json(users);
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Create/Update Route and Assign Bus/Driver (Unified Transactional Logic)
// @route   POST /api/admin/full-assignment
router.post('/full-assignment', protect, adminOnly, async (req, res) => {
  const { busNumber, driverId, routeName, stops } = req.body;

  try {
    // 1. Create or Update the Route
    let route = await Route.findOne({ routeName });
    if (!route) {
      route = new Route({ routeName, stops });
    } else {
      route.stops = stops; // Update stops for existing route
    }
    await route.save();

    // 2. Find or Create the Bus
    let bus = await Bus.findOne({ busNumber });
    if (!bus) {
      bus = new Bus({ busNumber, status: 'inactive' });
    }
    bus.driverId = driverId;
    bus.routeId = route._id;
    await bus.save();

    // 3. Update Driver's reference
    await User.findByIdAndUpdate(driverId, { assignedBusId: bus._id });

    res.json({ message: 'Bus, Driver, and Route synced successfully', busId: bus._id });
  } catch (error) {
    console.error('Full Assignment Error:', error);
    res.status(500).json({ message: 'Sync failed: ' + error.message });
  }
});

// @desc    Bulk Create Users
router.post('/users/bulk', protect, adminOnly, async (req, res) => {
    const usersData = req.body;
    const results = { created: 0, skipped: 0, errors: [] };

    for (const data of usersData) {
        try {
            const exists = await User.findOne({ email: data.email });
            if (exists) { results.skipped++; continue; }

            const { username, tempPassword } = generateTempCredentials(data.name);
            const salt = await bcrypt.genSalt(10);
            const hashedPassword = await bcrypt.hash(tempPassword, salt);

            await User.create({
                name: data.name,
                email: data.email,
                username,
                password: hashedPassword,
                role: data.role.toLowerCase(),
                isFirstLogin: true
            });
            await sendCredentialsEmail(data.email, data.name, username, tempPassword);
            results.created++;
        } catch (err) {
            results.errors.push({ email: data.email, error: err.message });
        }
    }
    res.json({ message: 'Bulk processing complete', results });
});

// @desc    Get all active buses for mapping/dashboard
router.get('/buses/active', protect, async (req, res) => {
    try {
        const buses = await Bus.find()
            .populate('routeId')
            .populate('driverId', 'name email');
        res.json(buses);
    } catch (error) {
        res.status(500).json({ message: 'Error loading buses' });
    }
});

// @desc    Get Complaints
router.get('/complaints', protect, adminOnly, async (req, res) => {
    const { status } = req.query;
    try {
        const filter = status ? { status } : {};
        const complaints = await Complaint.find(filter).sort({ createdAt: -1 });
        res.json(complaints);
    } catch (error) {
        res.status(500).json({ message: 'Error loading complaints' });
    }
});

// @desc    Update Complaint
router.patch('/complaints/:id', protect, adminOnly, async (req, res) => {
    try {
        const complaint = await Complaint.findByIdAndUpdate(req.params.id, req.body, { new: true });
        res.json({ message: 'Complaint updated', complaint });
    } catch (error) {
        res.status(500).json({ message: 'Update failed' });
    }
});

module.exports = router;
