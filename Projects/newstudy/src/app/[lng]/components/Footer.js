import React from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import { FaFacebookF, FaInstagram } from 'react-icons/fa';

const Footer = () => {
    return (
        <footer className="bg-dark text-center text-white">
            {/* Grid container */}
            <div className="container p-4 pb-0">
                {/* Section: Social media */}
                <section className="mb-4">
                    {/* Facebook */}
                    <a className="btn btn-outline-light btn-floating m-1" href="#!" role="button">
                        <FaFacebookF />
                    </a>
                    {/* Instagram */}
                    <a className="btn btn-outline-light btn-floating m-1" href="#!" role="button">
                        <FaInstagram />
                    </a>
                </section>
                {/* Section: Social media */}

                    <p className="text-start text-white mb-0">Contact: email@example.com</p>


            </div>
            {/* Grid container */}

            {/* Contact Info */}

            {/* Copyright */}
            <div className="text-center p-3" style={{ backgroundColor: 'rgba(0, 0, 0, 0.2)' }}>
                © 2024 Copyright:
                <p className="text-white mb-0">Kristijan Zafirovski</p>
            </div>
            {/* Copyright */}
        </footer>
    );
};

export default Footer;
