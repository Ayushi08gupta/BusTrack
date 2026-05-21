const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const dotenv = require('dotenv');

// Load models for socket db persistence
const Bus = require('./models/Bus');
const LiveLocation = require('./models/LiveLocation');

// Load environment variables
dotenv.config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE']
  }
});

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Socket.io Connection
io.on('connection', (socket) => {
  console.log(`User connected: ${socket.id}`);

  // Join a specific bus room
  socket.on('join:bus', (data) => {
    if (data && data.busId) {
      socket.join(data.busId);
      console.log(`Socket ${socket.id} joined bus room: ${data.busId}`);
    }
  });

  // Driver emits location update
  socket.on('location:update', async (data) => {
    // data should contain { busId, latitude, longitude }
    const { busId, latitude, longitude } = data;

    if (busId && latitude !== undefined && longitude !== undefined) {
      const timestamp = new Date();
      const payload = { busId, latitude, longitude, timestamp, status: 'active' };

      // Broadcast to everyone (students and admins) tracking this bus in real-time inside the room
      io.to(busId).emit('location:update', payload);

      // Persist to MongoDB asynchronously so it doesn't block the socket thread
      try {
        await Bus.findByIdAndUpdate(busId, {
          currentLocation: { latitude, longitude, timestamp },
          status: 'active'
        });

        await LiveLocation.create({
          busId,
          latitude,
          longitude,
          timestamp
        });
      } catch (err) {
        console.error('Error updating location via Socket:', err.message);
      }
    }
  });

  // Driver ends trip or goes offline
  socket.on('bus:offline', async (data) => {
    const { busId } = data;
    if (busId) {
      io.to(busId).emit('bus:offline', { busId });
      try {
        await Bus.findByIdAndUpdate(busId, {
          status: 'inactive'
        });
        console.log(`Bus ${busId} set to offline`);
      } catch (err) {
        console.error('Error setting bus offline via Socket:', err.message);
      }
    }
  });

  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.id}`);
  });
});

// Database connection
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/bustrack')
  .then(() => console.log('MongoDB connected successfully.'))
  .catch(err => console.error('MongoDB connection error:', err));

// Basic route
app.get('/', (req, res) => {
  res.send('BusTrack Backend is running');
});

// Import Routes
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const driverRoutes = require('./routes/driver');
const studentRoutes = require('./routes/student');

// Use Routes
app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/driver', driverRoutes);
app.use('/api/student', studentRoutes);

const PORT = process.env.PORT || 5000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`);
});
