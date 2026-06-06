const mongoose = require('mongoose');

const busSchema = new mongoose.Schema({
  busNumber: { type: String, required: true, unique: true },
  driverId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', default: null },
  routeId: { type: mongoose.Schema.Types.ObjectId, ref: 'Route', default: null },
  currentLocation: {
    latitude: { type: Number, default: null },
    longitude: { type: Number, default: null },
    speed: { type: Number, default: 0 },
    heading: { type: Number, default: 0 },
    timestamp: { type: Date, default: null }
  },
  status: { type: String, enum: ['active', 'inactive', 'maintenance'], default: 'inactive' }
}, { timestamps: true });

module.exports = mongoose.model('Bus', busSchema);
