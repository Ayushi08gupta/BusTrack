const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const Route = require('../models/Route');
const User = require('../models/User');
const { protect } = require('../middleware/authMiddleware');

// @desc    Get student's assigned bus info & real-time location
// @route   GET /api/student/bus-info
// @access  Private/Student
router.get('/bus-info', protect, async (req, res) => {
  if (req.user.role !== 'student') {
    return res.status(403).json({ message: 'Forbidden. Students only.' });
  }

  try {
    const user = await User.findById(req.user._id).populate('assignedBusId');
    if (!user.assignedBusId) {
      return res.status(404).json({ message: 'No bus assigned to this student yet' });
    }

    const bus = await Bus.findById(user.assignedBusId._id)
      .populate('driverId', 'name email username')
      .populate('routeId');

    if (!bus) {
      return res.status(404).json({ message: 'Assigned bus details not found' });
    }

    res.json(bus);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Search buses by route name
// @route   GET /api/student/search-buses
// @access  Private
router.get('/search-buses', protect, async (req, res) => {
  const { query } = req.query;
  try {
    // Find routes that match the query
    const routes = await Route.find({ 
      routeName: { $regex: query, $options: 'i' } 
    });
    
    const routeIds = routes.map(r => r._id);
    
    // Find buses assigned to those routes
    const buses = await Bus.find({ routeId: { $in: routeIds } })
      .populate('routeId')
      .populate('driverId', 'name');
      
    res.json(buses);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Get all active buses for mapping
// @route   GET /api/student/active-buses
// @access  Private
router.get('/active-buses', protect, async (req, res) => {
  try {
    const buses = await Bus.find({ status: 'active' })
      .populate('routeId')
      .select('currentLocation busNumber routeId status');
    res.json(buses);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Get specific bus location
// @route   GET /api/bus/location/:id
// @access  Private
router.get('/bus/location/:id', protect, async (req, res) => {
  try {
    const bus = await Bus.findById(req.params.id)
      .populate('routeId')
      .select('currentLocation status busNumber routeId');
    if (!bus) {
      return res.status(404).json({ message: 'Bus not found' });
    }
    res.json(bus);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Get all routes
// @route   GET /api/routes
// @access  Private
router.get('/routes', protect, async (req, res) => {
  try {
    const routes = await Route.find();
    res.json(routes);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
});

module.exports = router;
