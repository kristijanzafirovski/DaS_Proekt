'use client';

import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'animate.css';
import { initializeTranslation } from '@/app/[lng]/components/LanguageSwitcher';
import { use } from 'react';
import {FaFacebookF, FaInstagram} from "react-icons/fa";

export default function Home({ params }) {
    const [t, setT] = useState(() => () => '');
    const unwrappedParams = use(params);

    useEffect(() => {
        async function fetchTranslation() {
            const translationFunction = await initializeTranslation(unwrappedParams.lng, 'button');
            setT(() => translationFunction);
        }

        fetchTranslation();
    }, [unwrappedParams.lng]);

    return (
        <>

        <Container fluid className="bg-sec text-dark p-5 d-flex flex-column flex-md-row" style={{ minHeight: '40em' }}>
            <Container fluid>
                <h1 className="mb-4 display-3">{t('who_are_we.title')}</h1>
                <hr />
                <p className="lead">{t('who_are_we.p1')}</p>
                <p className="lead">{t('who_are_we.p2')}</p>
            </Container>
        <img src='/s1.jpg' className="img-fluid" style={{width: 800 + "px"}}/>
            </Container>
            <Container fluid className="bg-th text-dark p-5 d-flex flex-column " style={{minHeight: '40em'}}>
                <h1 className="mb-4 display-3">{t('services.title')}</h1>
                <Row className="gy-4 justify-content-center">
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl1')}</strong>
                                </Card.Title>
                                <Card.Text className="lead">{t('services.l1')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl2')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l2')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl3')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l3')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl4')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l4')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl5')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l5')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl6')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l6')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                    <Col md={4}>
                        <Card className="bg-transparent h-100">
                            <Card.Body>
                                <Card.Title className="text-center fs-3">
                                    <strong>{t('services.tl7')}</strong></Card.Title>
                                <Card.Text className="lead">{t('services.l7')}</Card.Text>
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
            <Container fluid className="bg-primary-subtle text-dark text-center p-5 d-flex flex-column" style={{minHeight: '40em'}}>
                <h1 className="mb-4 display-3">{t('why_choose_us.title')}</h1>
                <div className="list-unstyled">
                    <div className="mb-4">
                        <h3>{t('why_choose_us.t1')}</h3>
                        <p className="lead">{t('why_choose_us.d1')}</p>
                    </div>
                    <div className="mb-4">
                        <h3>{t('why_choose_us.t2')}</h3>
                        <p className="lead">{t('why_choose_us.d2')}</p>
                    </div>
                    <div className="mb-4">
                        <h3>{t('why_choose_us.t3')}</h3>
                        <p className="lead">{t('why_choose_us.d3')}</p>
                    </div>
                    <div className="mb-4">
                        <h3>{t('why_choose_us.t4')}</h3>
                        <p className="lead">{t('why_choose_us.d4')}</p>
                    </div>
                </div>
                <p className="display-2 text-dark">{t('why_choose_us.t5')}</p>
                <p className="fw-lighter fs-2 w-75 m-auto">{t('why_choose_us.d5')}</p>
            </Container>
            {/*Macedonia*/}
            <Container fluid className="bg-sec text-dark p-5 d-flex flex-column" style={{minHeight: '40em'}}>
                <h1 className="mb-4 display-3 text-center">{t('macedonia.title')}</h1>
                <hr/>
                <Container fluid className="d-flex">
                    <Container fluid className="p-xxl-5 ">
                        <p className="lead fs-3">{t('macedonia.p1')}</p>
                        <p className="lead fs-3">{t('macedonia.p2')}</p>
                    </Container>

                </Container>
                {/*Skopje*/}
                <Container fluid className="skopje-bckg mt-5" style={{height: 30 + "em"}}>
                    <p className="display-2 text-white text-center" style={{paddingTop: 200 + "px"}}>
                        <strong>{t('macedonia.t1')}</strong></p>
                    <hr className="border-5 border-top w-75 mx-auto"/>
                </Container>
                <Container fluid className="d-flex  bg-primary-subtle">
                    <Container fluid className="p-xxl-5">
                        <p className="lead fs-3">{t('macedonia.p3')}</p>
                        <p className="lead fs-3">{t('macedonia.p4')}</p>
                        <p className="lead fs-1 text-center">{t('macedonia.t2')}</p>
                        <p className="lead fs-3">{t('macedonia.p5')}</p>
                        <p className="lead fs-3">{t('macedonia.p6')}</p>
                    </Container>
                </Container>
            </Container>
            <Container fluid className="bg-sec text-dark p-5 d-flex flex-column w-100" style={{minHeight: '40em'}}>
                <h1 className="mb-4 display-3 text-center">{t('uni.title')}</h1>
                <Row className="gy-4">
                    <Col md={6}>
                        <Card className="h-100 text-center">
                            <div className="">
                                <img src="/en/ukim.jpg" className="img-fluid img-thumbnail"/>
                            </div>
                            <div className="m-5">
                                <h3 className="fw-bold">{t('uni.u1t')}</h3>
                                <p className="fw-lighter fs-4">{t('uni.u1d')}</p>
                                <p className="fw-lighter fs-4">{t('uni.u1d2')}</p>
                                <h3 className="fw-bold" style={{marginTop: 80 + "px"}}>{t('uni.u2t')}</h3>
                                <p className="fw-lighter fs-4">{t('uni.u2d')}</p>
                                <p className="fw-lighter fs-4">{t('uni.u2d2')}</p>
                            </div>
                        </Card>
                    </Col>
                    <Col md={6}>
                        <Card className="h-100 text-center">
                            <div className="">
                                <img src="/en/utms.webp" className="img-fluid img-thumbnail"/>
                            </div>
                            <div className="m-5">
                                <h3 className="fw-bold">{t('uni.u3t')}</h3>
                                <p className="fw-lighter fs-4">{t('uni.u3d')}</p>
                                <p className="fw-lighter fs-4">{t('uni.u3d2')}</p>
                            </div>
                        </Card>
                    </Col>
                </Row>
            </Container>
            <Container fluid className="bg-th text-dark p-5 d-flex flex-column align-items-center" style={{ minHeight: '40em' }}>
                <h1 className="mb-4 display-3 w-100 text-center">{t('contact.title')}</h1>
                <hr className="w-100" />
                <Row className="gy-4 w-100">
                    <Col xs={12} md={6} className="p-5 my-5 border border-5 border-black rounded-start bg-transparent">
                        <h2 className="mb-4 text-center">{t('contact.formTitle')}</h2>
                        <form>
                            <div className="mb-3">
                                <label htmlFor="name" className="form-label">{t('contact.formName')}</label>
                                <input type="text" className="form-control" id="name" placeholder={t('contact.formNamePlaceholder')} />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="email" className="form-label">Email</label>
                                <input type="email" className="form-control" id="email" placeholder="name@example.com" />
                            </div>
                            <div className="mb-3">
                                <label htmlFor="message" className="form-label">{t('contact.formMsg')}</label>
                                <textarea className="form-control" id="message" rows="4" style={{ resize: "none" }}></textarea>
                            </div>
                            <button type="submit" className="btn btn-outline-dark m-1">Submit</button>
                        </form>
                    </Col>
                    <Col xs={12} md={6} className="p-5 my-5 border border-5 border-black rounded-end bg-transparent d-flex flex-column align-items-center">
                        <h2 className="mb-4 text-center">{t('contact.social')}</h2>
                        <div className="d-flex align-items-center">
                            <a className="btn btn-outline-dark m-5" href="#!" role="button">
                                <FaFacebookF />
                            </a>
                            <a className="btn btn-outline-dark btn-floating m-5" href="#!" role="button">
                                <FaInstagram />
                            </a>
                        </div>
                    </Col>
                </Row>
            </Container>

        </>
    );
}


