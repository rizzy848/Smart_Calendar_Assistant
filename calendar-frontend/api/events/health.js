module.exports = function handler(req, res) {
    res.status(200).json({
        status: 'OK',
        aiParserAvailable: !!process.env.GROQ_API_KEY,
        timestamp: Date.now()
    });
};
