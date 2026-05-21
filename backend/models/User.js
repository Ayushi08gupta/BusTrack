const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  username: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  role: { type: String, enum: ['admin', 'student', 'driver'], required: true },
  isFirstLogin: { type: Boolean, default: true },
  isActive: { type: Boolean, default: true },
  assignedBusId: { type: mongoose.Schema.Types.ObjectId, ref: 'Bus', default: null },
  resetPasswordToken: String,
  resetPasswordExpires: Date
}, { timestamps: true });

module.exports = mongoose.model('User', userSchema);
