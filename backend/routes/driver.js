const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const Trip = require('../models/Trip');
const LiveLocation = require('../models/LiveLocation');
const { protect } = require('../middleware/authMiddleware');

// @desc    Get driver's assigned bus and route details
// @route   GET /api/driver/assigned-bus
// @access  Private/Driver
router.get('/assigned-bus', protect, async (req, res) => {
  console.log('Fetching assignment for user:', req.user._id, 'Role:', req.user.role);
  if (req.user.role !== 'driver') {
    console.log('Access denied: Role is not driver');
    return res.status(403).json({ message: 'Forbidden. Drivers only.' });
  }

  try {
    const bus = await Bus.findOne({ driverId: req.user._id })
      .populate('routeId');

    if (!bus) {
      console.log('No bus found for driverId:', req.user._id);
      return res.status(404).json({ message: 'No bus assigned to this driver' });
    }

    // Ensure driverId is the full user object to match Android model
    bus.driverId = req.user;

    console.log('Found bus:', bus.busNumber);
    res.json(bus);
  } catch (error) {
    console.error('Error in /assigned-bus:', error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Start Journey
// @route   POST /api/driver/start-journey
router.post('/start-journey', protect, async (req, res) => {
    const { busId, latitude, longitude } = req.body;
    try {
        const bus = await Bus.findById(busId);
        if (!bus) return res.status(404).json({ message: 'Bus not found' });

        bus.status = 'active';
        if (latitude !== undefined && longitude !== undefined) {
            bus.currentLocation = {
                latitude,
                longitude,
                timestamp: new Date()
            };
        }
        await bus.save();

        const trip = await Trip.create({
            busId,
            driverId: req.user._id,
            routeId: bus.routeId,
            status: 'active'
        });

        res.status(201).json(trip);
    } catch (error) {
        console.error('Error starting journey:', error);
        res.status(500).json({ message: 'Server error' });
    }
});

// @desc    Stop Journey
// @route   POST /api/driver/stop-journey
router.post('/stop-journey', protect, async (req, res) => {
    const { busId, tripId } = req.body;
    try {
        const bus = await Bus.findById(busId);
        if (bus) {
            bus.status = 'inactive';
            await bus.save();
        }

        if (tripId) {
            await Trip.findByIdAndUpdate(tripId, {
                status: 'completed',
                endTime: new Date()
            });
        }

        res.json({ message: 'Journey stopped successfully' });
    } catch (error) {
        res.status(500).json({ message: 'Server error' });
    }
});

// @desc    Update bus location & status (REST backup)
// @route   POST /api/driver/location-update
// @access  Private/Driver
router.post('/location-update', protect, async (req, res) => {
  const { latitude, longitude, status } = req.body;

  if (req.user.role !== 'driver') {
    return res.status(403).json({ message: 'Forbidden. Drivers only.' });
  }

  if (latitude === undefined || longitude === undefined) {
    return res.status(400).json({ message: 'Latitude and longitude are required' });
  }

  try {
    const bus = await Bus.findOne({ driverId: req.user._id });
    if (!bus) {
      return res.status(404).json({ message: 'No bus assigned to this driver' });
    }

    const timestamp = new Date();

    // Update current location in Bus model
    bus.currentLocation = {
      latitude,
      longitude,
      timestamp
    };

    if (status) {
      bus.status = status;
    }
    await bus.save();

    // Save to LiveLocation log
    await LiveLocation.create({
      busId: bus._id,
      latitude,
      longitude,
      timestamp
    });

    res.json({ message: 'Location updated successfully', currentLocation: bus.currentLocation });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

module.exports = router;
