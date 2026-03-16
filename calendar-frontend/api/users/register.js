const crypto = require('crypto');

module.exports = async function handler(req, res) {
    if (req.method !== 'POST') return res.status(405).end();

    const { username, email } = req.body;

    if (!username || !email) {
        return res.status(400).json({ error: 'Username and email are required' });
    }

    // Generate a deterministic userId from email — no database needed
    const userId = crypto.createHash('md5').update(email.toLowerCase().trim()).digest('hex').substring(0, 8);

    return res.status(200).json({ userId, username, email });
};
