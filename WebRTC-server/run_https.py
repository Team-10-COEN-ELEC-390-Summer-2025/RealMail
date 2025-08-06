import uvicorn
import os
from main import app

if __name__ == "__main__":
    # Path to your SSL certificates
    ssl_keyfile = os.path.join(os.path.dirname(__file__), "key.pem")
    ssl_certfile = os.path.join(os.path.dirname(__file__), "cert.pem")

    # Check if the certificates exist
    if not os.path.exists(ssl_keyfile) or not os.path.exists(ssl_certfile):
        print("SSL certificates not found. Generating self-signed certificates...")

        # Generate self-signed certificates for development
        from OpenSSL import crypto

        # Create a key pair
        k = crypto.PKey()
        k.generate_key(crypto.TYPE_RSA, 2048)

        # Create a self-signed cert
        cert = crypto.X509()
        cert.get_subject().C = "US"
        cert.get_subject().ST = "State"
        cert.get_subject().L = "Locality"
        cert.get_subject().O = "Organization"
        cert.get_subject().OU = "Organizational Unit"
        cert.get_subject().CN = "localhost"
        cert.set_serial_number(1000)
        cert.gmtime_adj_notBefore(0)
        cert.gmtime_adj_notAfter(10*365*24*60*60)  # 10 years
        cert.set_issuer(cert.get_subject())
        cert.set_pubkey(k)
        cert.sign(k, 'sha256')

        # Save the certificate
        with open(ssl_certfile, "wb") as f:
            f.write(crypto.dump_certificate(crypto.FILETYPE_PEM, cert))

        # Save the private key
        with open(ssl_keyfile, "wb") as f:
            f.write(crypto.dump_privatekey(crypto.FILETYPE_PEM, k))

        print(f"Self-signed certificates generated at {ssl_certfile} and {ssl_keyfile}")
    else:
        print(f"Using existing SSL certificates at {ssl_certfile} and {ssl_keyfile}")

    # Get the host and port from environment variables if available
    host = "127.0.0.1"  # Using localhost instead of 0.0.0.0 for better browser compatibility
    port = 8443

    print(f"Starting HTTPS server at https://{host}:{port}")
    print(f"Note: Use 'localhost:{port}' in your browser, not the IP address")

    try:
        # Run the server with SSL
        uvicorn.run(
            "main:app",
            host=host,
            port=port,
            ssl_keyfile=ssl_keyfile,
            ssl_certfile=ssl_certfile,
            reload=True,
            log_level="info"
        )
    except Exception as e:
        print(f"Error starting the server: {e}")
        import traceback
        traceback.print_exc()
