import Link from 'next/link';
import { languages } from '@/app/i18n/settings';
import { useTranslation } from '@/app/i18n';
import 'animate.css';

export async function initializeTranslation(lng, ns)
{
    const { t } = await useTranslation(lng, ns);
    return t;
}
export const SpanLanguage = async ({ lng }) => {
    const { t } = initializeTranslation(lng);

    return (
        <div className="bg-body text-white p-2 rounded-pill d-inline-flex animate__animated animate__fadeIn " >
            {languages.filter((l) => lng !== l).map((l, index) => (
                <span key={l} className="text-decoration-none text-black">
                    {index>0 &&('/')}
                    <Link href={`/${l}`} passHref>
                        <span className="text-decoration-none text-black">{
                            l.toUpperCase()
                        }</span>
                    </Link>
                </span>
            ))}
        </div>
    );
};
