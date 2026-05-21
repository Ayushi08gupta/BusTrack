const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const LiveLocation = require('../models/LiveLocation');
const { protect } = require('../middleware/authMiddleware');

// @desc    Get driver's assigned bus and route details
// @route   GET /api/driver/assigned-bus
// @access  Private/Driver
router.get('/assigned-bus', protect, async (req, res) => {
  if (req.user.role !== 'driver') {
    return res.status(403).json({ message: 'Forbidden. Drivers only.' });
  }

  try {
    const bus = await Bus.findOne({ driverId: req.user._id })
      .populate('routeId');

    if (!bus) {
      return res.status(404).json({ message: 'No bus assigned to this driver' });
    }

    res.json(bus);
  } catch (error) {
    console.error(error);
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
