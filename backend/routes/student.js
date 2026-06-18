const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const Route = require('../models/Route');
const User = require('../models/User');
const Complaint = require('../models/Complaint');
const { protect } = require('../middleware/authMiddleware');

// @desc    Get student's assigned bus info
router.get('/bus-info', protect, async (req, res) => {
  try {
    const user = await User.findById(req.user._id).populate('assignedBusId');
    if (!user.assignedBusId) return res.status(404).json({ message: 'No bus assigned' });

    const bus = await Bus.findById(user.assignedBusId).populate('driverId', 'name').populate('routeId');
    res.json(bus);
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Search buses
router.get('/search-buses', protect, async (req, res) => {
  const { query } = req.query;
  try {
    const buses = await Bus.find({ busNumber: { $regex: query, $options: 'i' } }).populate('routeId');
    res.json(buses);
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

// @desc    Submit a complaint
// @route   POST /api/student/complaints
router.post('/complaints', protect, async (req, res) => {
  const { type, description, busId } = req.body;
  try {
    const complaint = await Complaint.create({
      studentId: req.user._id,
      studentName: req.user.name,
      busId,
      type,
      description,
      status: 'pending'
    });
    res.status(201).json({ message: 'Complaint submitted successfully', complaint });
  } catch (error) {
    res.status(500).json({ message: 'Failed to submit complaint' });
  }
});

module.exports = router;
