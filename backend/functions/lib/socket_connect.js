"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
// This code sets up a simple Socket.IO server that listens for connections
const express_1 = __importDefault(require("express"));
const http_1 = __importDefault(require("http"));
const socket_io_1 = require("socket.io");
const cors_1 = __importDefault(require("cors"));
const app = (0, express_1.default)();
app.use((0, cors_1.default)());
app.use(express_1.default.json());
const server = http_1.default.createServer(app);
const io = new socket_io_1.Server(server, {
    cors: {
        origin: "*", // * to be reviews later
    },
});
let connectedClients = {};
// let connectedClients = {};
io.on("connection", (socket) => {
    console.log("Client connected:", socket.id);
    // Store socket by client ID (you can use user ID instead)
    connectedClients[socket.id] = socket;
    socket.on("disconnect", () => {
        console.log("Client disconnected:", socket.id);
        delete connectedClients[socket.id];
    });
});
// POST endpoint to trigger message to all connected clients
app.post("/notify", (req, res) => {
    const { message } = req.body;
    console.log("Sending message:", message);
    Object.values(connectedClients).forEach((socket) => {
        socket.emit("serverNotification", { message });
    });
    res.send({ status: "Sent to all connected clients." });
});
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
});
//# sourceMappingURL=socket_connect.js.map