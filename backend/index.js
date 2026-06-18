const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const dotenv = require('dotenv');

const Bus = require('./models/Bus');
const LiveLocation = require('./models/LiveLocation');

dotenv.config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] }
});

app.use(cors());
app.use(express.json());

// Socket.io Logic (Maintained as is for real-time tracking)
io.on('connection', (socket) => {
  socket.on('join:bus', (data) => { if (data.busId) socket.join(data.busId); });
  socket.on('location:update', async (data) => {
    const { busId, latitude, longitude } = data;
    if (busId && latitude && longitude) {
      io.to(busId).emit('location:update', data);
      try {
        await Bus.findByIdAndUpdate(busId, { 
          currentLocation: { latitude, longitude, timestamp: new Date() },
          status: 'active' 
        });
      } catch (err) { console.error('Socket DB Error:', err.message); }
    }
  });
});

// Routes
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const driverRoutes = require('./routes/driver');
const studentRoutes = require('./routes/student');
const busRoutes = require('./routes/bus'); // Fixed: Added missing unified bus routes

app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/driver', driverRoutes);
app.use('/api/student', studentRoutes);
app.use('/api/bus', busRoutes); // Fixed: Registered generic bus endpoint

mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/bustrack')
  .then(() => console.log('MongoDB connected.'))
  .catch(err => console.error(err));

const PORT = 5001;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
});
