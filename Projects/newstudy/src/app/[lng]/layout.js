import { dir } from 'i18next';
import { languages } from '../i18n/settings';
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../scss/main.scss';
import 'animate.css';
import Footer from "@/app/[lng]/components/Footer";
import {Navbar} from "@/app/[lng]/components/Navbar";

export async function generateStaticParams() {
    return languages.map((lng) => ({ lng }));
}

export default function RootLayout({ children, params }) {
    const { lng } = params;

    return (
        <html lang={lng} dir={dir(lng)}>
        <head>
            <title>StudyBridge Macedonia</title>
        </head>
        <body className="bg-primary sora">
        <Navbar lng={lng} />
        <main className="">
            {children}
        </main>
        <Footer />
        </body>
        </html>
    );
}
