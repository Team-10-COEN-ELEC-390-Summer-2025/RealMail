"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// this file is used for initializing the database connection
const pg_1 = require("pg");
const pool = new pg_1.Pool({
    user: process.env.DB_USER,
    host: process.env.DB_HOST,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    port: 5432,
});
exports.default = pool;
//# sourceMappingURL=db_connect.js.map