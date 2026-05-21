const mongoose = require('mongoose');
const bcrypt = require('bcrypt');
const dotenv = require('dotenv');
const User = require('./models/User');

dotenv.config();

const seedAdmin = async () => {
  const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/bustrack';
  const maskedUri = uri.replace(/\/\/.*@/, '//****:****@'); // Hide password in logs
  
  console.log(`Attempting to connect to: ${maskedUri}`);

  try {
    await mongoose.connect(uri);
    console.log('MongoDB connected successfully.');

    const adminEmail = 'admin@bustrack.com';
    const adminExists = await User.findOne({ email: adminEmail });

    if (adminExists) {
      console.log('Admin user already exists. Seeding skipped.');
      process.exit(0);
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash('admin123', salt);

    await User.create({
      name: 'System Admin',
      email: adminEmail,
      username: 'admin',
      password: hashedPassword,
      role: 'admin',
      isFirstLogin: false
    });

    console.log('----------------------------------------------------');
    console.log('SUCCESS: Admin user created successfully!');
    console.log(`Login Email: ${adminEmail}`);
    console.log('Password: admin123');
    console.log('----------------------------------------------------');
    process.exit(0);
  } catch (error) {
    console.error('Error seeding admin user:', error.message);
    if (error.message.includes('authentication failed')) {
        console.log('\nTIP: Your MongoDB Atlas password in .env is incorrect.');
        console.log('Check your Atlas "Database Access" settings.');
    }
    process.exit(1);
  }
};

seedAdmin();
