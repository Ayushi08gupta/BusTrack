const mongoose = require('mongoose');

const stopSchema = new mongoose.Schema({
  name: { type: String, required: true },
  latitude: { type: Number, required: true },
  longitude: { type: Number, required: true },
  eta: { type: String } // e.g. '10:30 AM'
});

const routeSchema = new mongoose.Schema({
  routeName: { type: String, required: true, unique: true },
  stops: [stopSchema],
  distance: { type: Number }, // in km
  estimatedTime: { type: Number } // in minutes
}, { timestamps: true });

module.exports = mongoose.model('Route', routeSchema);
