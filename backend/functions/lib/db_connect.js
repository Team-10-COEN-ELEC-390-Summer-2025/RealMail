"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
// this file is used for initializing the database connection
const pg_1 = require("pg");
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
let pool;
const getPool = () => {
    if (!pool) {
        console.log("Creating new database connection pool.");
        console.log("DB_NAME:", process.env.DB_NAME); // Add this for debugging
        pool = new pg_1.Pool({
            user: process.env.DB_USER,
            host: process.env.DB_HOST,
            database: process.env.DB_NAME,
            password: process.env.DB_PASSWORD,
            port: 5432,
        });
    }
    return pool;
};
exports.default = getPool;
//# sourceMappingURL=db_connect.js.map