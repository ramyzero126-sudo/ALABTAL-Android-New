require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'alabtal-secret-change-me';
const PORT = process.env.PORT || 3000;

// حساب تجريبي
const users = [
  {
    id: '1',
    username: 'admin',
    password: bcrypt.hashSync('admin123', 10),
    role: 'master',
    name: 'المسؤول'
  }
];

let riders = [];

// تسجيل الدخول
app.post('/api/login', async (req, res) => {
  const { username, password } = req.body;
  const user = users.find(u => u.username === username);

  if (!user || !(await bcrypt.compare(password, user.password))) {
    return res.status(401).json({ ok: false, error: 'اسم المستخدم أو كلمة المرور غلط' });
  }

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    JWT_SECRET,
    { expiresIn: '12h' }
  );

  res.json({
    ok: true,
    token,
    user: {
      id: user.id,
      username: user.username,
      role: user.role,
      name: user.name
    }
  });
});

// حماية الروتات
function auth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;

  if (!token) {
    return res.status(401).json({ ok: false, error: 'محتاج تسجيل دخول' });
  }

  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ ok: false, error: 'الجلسة انتهت' });
  }
}

// جلب المناديب
app.get('/api/riders', auth, (req, res) => {
  res.json({ ok: true, riders });
});

// إضافة مندوب
app.post('/api/riders', auth, (req, res) => {
  const { name, phone, city, zone } = req.body;

  if (!name) {
    return res.status(400).json({ ok: false, error: 'الاسم مطلوب' });
  }

  const rider = {
    id: uuidv4().slice(0, 8),
    name,
    phone: phone || '',
    city: city || '',
    zone: zone || '',
    createdAt: new Date().toISOString()
  };

  riders.push(rider);
  res.json({ ok: true, rider });
});

// حذف مندوب
app.delete('/api/riders/:id', auth, (req, res) => {
  const before = riders.length;
  riders = riders.filter(r => r.id !== req.params.id);
  res.json({ ok: true, deleted: before !== riders.length });
});

// حالة السيرفر
app.get('/api/health', (req, res) => {
  res.json({
    ok: true,
    service: 'ALABTAL Backend',
    ridersCount: riders.length
  });
});

app.listen(PORT, () => {
  console.log('ALABTAL Backend شغال على http://localhost:' + PORT);
  console.log('حساب تجريبي → admin / admin123');
});
