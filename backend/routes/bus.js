const express = require('express');
const router = express.Router();
const Bus = require('../models/Bus');
const { protect } = require('../middleware/authMiddleware');

// @desc    Get single bus full details
// @route   GET /api/bus/:id
router.get('/:id', protect, async (req, res) => {
  try {
    const bus = await Bus.findById(req.params.id)
      .populate('driverId', 'name email username')
      .populate('routeId');

    if (!bus) {
      return res.status(404).json({ message: 'Bus not found' });
    }

    res.json(bus);
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

module.exports = router;
