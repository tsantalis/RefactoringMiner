package de.adorsys.xs2a.adapter.sparkasse;

import de.adorsys.xs2a.adapter.api.PaymentInitiationService;
import de.adorsys.xs2a.adapter.api.RequestParams;
import de.adorsys.xs2a.adapter.api.Response;
import de.adorsys.xs2a.adapter.api.model.*;
import de.adorsys.xs2a.adapter.test.ServiceWireMockTest;
import de.adorsys.xs2a.adapter.test.TestRequestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ServiceWireMockTest(SparkasseServiceProvider.class)
class SparkassePaymentInitiationServiceWireMockTest {
    protected static final String PAYMENT_ID = "5f6e3778-2b5c-460c-90f9-c86b0f5c5d57";
    protected static final String AUTHORISATION_ID = "c9ef5300-0091-47c6-9df9-2190313e8f03";
    private final PaymentInitiationService service;

    SparkassePaymentInitiationServiceWireMockTest(PaymentInitiationService service) {
        this.service = service;
    }

    @Test
    void initiatePayment() throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/payments/sepa-credit-transfers/initiate-payment.json");

        Response<PaymentInitationRequestResponse201> response = service.initiatePayment(PaymentService.PAYMENTS,
            PaymentProduct.SEPA_CREDIT_TRANSFERS,
            requestResponse.requestHeaders(),
            RequestParams.empty(),
            requestResponse.requestBody(PaymentInitiationJson.class));

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(PaymentInitationRequestResponse201.class));
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void authenticatePsu(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/authenticate-psu.json");

        Response<StartScaprocessResponse> response = service.startPaymentAuthorisation(paymentService,
            paymentProduct,
            PAYMENT_ID,
            requestResponse.requestHeaders(),
            RequestParams.empty(),
            requestResponse.requestBody(UpdatePsuAuthentication.class));

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(StartScaprocessResponse.class));
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void selectScaMethod(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/select-sca-method.json");

        Response<SelectPsuAuthenticationMethodResponse> response = service.updatePaymentPsuData(paymentService,
            paymentProduct,
            PAYMENT_ID,
            AUTHORISATION_ID,
            requestResponse.requestHeaders(),
            RequestParams.empty(),
            requestResponse.requestBody(SelectPsuAuthenticationMethod.class));

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(SelectPsuAuthenticationMethodResponse.class));
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void authoriseTransaction(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/authorise-transaction.json");

        Response<ScaStatusResponse> response = service.updatePaymentPsuData(paymentService,
            paymentProduct,
            PAYMENT_ID,
            AUTHORISATION_ID,
            requestResponse.requestHeaders(),
            RequestParams.empty(),
            requestResponse.requestBody(TransactionAuthorisation.class));

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(ScaStatusResponse.class));
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void getScaStatus(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/get-sca-status.json");

        Response<ScaStatusResponse> response = service.getPaymentInitiationScaStatus(paymentService,
            paymentProduct,
            PAYMENT_ID,
            AUTHORISATION_ID,
            requestResponse.requestHeaders(),
            RequestParams.empty());

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(ScaStatusResponse.class));
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void getPaymentStatus(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/get-payment-status.json");

        Response<PaymentInitiationStatusResponse200Json> response = service.getPaymentInitiationStatus(paymentService,
            paymentProduct,
            PAYMENT_ID,
            requestResponse.requestHeaders(),
            RequestParams.empty());

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(PaymentInitiationStatusResponse200Json.class));
    }

    private static Stream<Arguments> paymentTypes() {
        return Stream.of(arguments(PaymentService.PAYMENTS, PaymentProduct.SEPA_CREDIT_TRANSFERS));
    }
}
