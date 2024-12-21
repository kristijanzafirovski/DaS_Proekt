import 'bootstrap/dist/css/bootstrap.min.css';
import { Container, Nav, Navbar as BootstrapNavbar, NavbarBrand, NavLink, NavbarToggle, NavbarCollapse } from 'react-bootstrap';
import { useTranslation } from '@/app/i18n';
import { SpanLanguage } from '@/app/[lng]/components/LanguageSwitcher';

export const Navbar = async ({ lng }) => {
    const { t } = await useTranslation(lng, 'button');

    return (
        <BootstrapNavbar bg="primary" expand="md" className="pt-5 d-flex justify-content-center align-items-start classroom-bckg" style={{ height: '30em' }} collapseOnSelect>
            <Container className="mx-0 container-fluid w-100 flex-column animate__animated animate__backInDown">
                <NavbarBrand className="mb-0">
                    <img
                        src='/en/logo.png'
                        height='120'
                        alt='Logo'
                        loading='lazy'
                        className="d-block"
                    />
                </NavbarBrand>
                <NavbarBrand className="text-center mb-0">
                    <h1 className="text-white"><strong>StudyBridge Macedonia</strong></h1>
                </NavbarBrand>
                <NavbarToggle aria-controls="responsive-navbar-nav" />
                <NavbarCollapse id="responsive-navbar-nav">
                    <Nav className="mt-3 justify-content-center">
                        <NavLink href="#home" className="trigger bg-body rounded-start-pill mx-1">{t('navigation.who_are_we')}</NavLink>
                        <NavLink href="#link" className="bg-body mx-1">{t('navigation.our_services')}</NavLink>
                        <NavLink href="#home" className="bg-body mx-1">{t('navigation.why_choose_us')}</NavLink>
                        <NavLink href="#link" className="bg-body mx-1">{t('navigation.education_in_macedonia')}</NavLink>
                        <NavLink href="#home" className="bg-body mx-1">{t('navigation.universities')}</NavLink>
                        <NavLink href="#home" className="bg-body rounded-end-pill mx-1">{t('navigation.get_in_contact')}</NavLink>
                    </Nav>
                </NavbarCollapse>
            </Container>
            <SpanLanguage />
        </BootstrapNavbar>
    );
};
