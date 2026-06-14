const mongoose = require('mongoose');

const complaintSchema = new mongoose.Schema({
  studentId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  studentName: String,
  busId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Bus'
  },
  type: {
    type: String,
    required: true,
    enum: ['Delay', 'Behavior', 'Maintenance', 'Other']
  },
  description: {
    type: String,
    required: true
  },
  status: {
    type: String,
    enum: ['pending', 'in-progress', 'resolved'],
    default: 'pending'
  },
  remarks: {
    type: String,
    default: ''
  }
}, { timestamps: true });

module.exports = mongoose.model('Complaint', complaintSchema);
