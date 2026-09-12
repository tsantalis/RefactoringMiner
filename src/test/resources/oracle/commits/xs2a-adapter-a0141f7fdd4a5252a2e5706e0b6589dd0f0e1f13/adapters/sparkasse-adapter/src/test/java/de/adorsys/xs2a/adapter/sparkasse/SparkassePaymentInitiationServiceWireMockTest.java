package de.adorsys.xs2a.adapter.sparkasse;

import de.adorsys.xs2a.adapter.api.PaymentInitiationService;
import de.adorsys.xs2a.adapter.api.RequestParams;
import de.adorsys.xs2a.adapter.api.Response;
import de.adorsys.xs2a.adapter.api.model.*;
import de.adorsys.xs2a.adapter.test.ServiceWireMockTest;
import de.adorsys.xs2a.adapter.test.TestRequestResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ServiceWireMockTest(SparkasseServiceProvider.class)
class SparkassePaymentInitiationServiceWireMockTest {
    protected static final String SCT_PAYMENT_ID = "5f6e3778-2b5c-460c-90f9-c86b0f5c5d57";
    protected static final String SCT_AUTHORISATION_ID = "c9ef5300-0091-47c6-9df9-2190313e8f03";
    protected static final String PAIN_SCT_PAYMENT_ID = "850987d5-eb54-4053-84c1-945485147e3b";
    protected static final String PAIN_SCT_AUTHORISATION_ID = "cafd117d-2969-48be-b003-acd835bb02e6";
    private final PaymentInitiationService service;
    private final Map<PaymentProduct, Pair<String, String>> ids;

    SparkassePaymentInitiationServiceWireMockTest(PaymentInitiationService service) {
        this.service = service;
        this.ids = initiateMap();
    }

    private Map<PaymentProduct, Pair<String, String>> initiateMap() {
        Map<PaymentProduct, Pair<String, String>> map = new HashMap<>();
        map.put(PaymentProduct.SEPA_CREDIT_TRANSFERS, Pair.of(SCT_PAYMENT_ID, SCT_AUTHORISATION_ID));
        map.put(PaymentProduct.PAIN_001_SEPA_CREDIT_TRANSFERS, Pair.of(PAIN_SCT_PAYMENT_ID, PAIN_SCT_AUTHORISATION_ID));
        return map;
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void initiatePayment(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/initiate-payment.json");

        Response<PaymentInitationRequestResponse201> response = service.initiatePayment(paymentService,
            paymentProduct,
            requestResponse.requestHeaders(),
            RequestParams.empty(),
            isJson(paymentProduct) ? requestResponse.requestBody(PaymentInitiationJson.class) : requestResponse.requestBody());

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(PaymentInitationRequestResponse201.class));
    }

    private boolean isJson(PaymentProduct paymentProduct) {
        return paymentProduct == PaymentProduct.SEPA_CREDIT_TRANSFERS;
    }

    @ParameterizedTest
    @MethodSource("paymentTypes")
    void authenticatePsu(PaymentService paymentService, PaymentProduct paymentProduct) throws Exception {
        TestRequestResponse requestResponse =
            new TestRequestResponse("pis/" + paymentService + "/" + paymentProduct + "/authenticate-psu.json");

        Response<StartScaprocessResponse> response = service.startPaymentAuthorisation(paymentService,
            paymentProduct,
            ids.get(paymentProduct).getLeft(),
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
            ids.get(paymentProduct).getLeft(),
            ids.get(paymentProduct).getRight(),
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
            ids.get(paymentProduct).getLeft(),
            ids.get(paymentProduct).getRight(),
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
            ids.get(paymentProduct).getLeft(),
            ids.get(paymentProduct).getRight(),
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
            ids.get(paymentProduct).getLeft(),
            requestResponse.requestHeaders(),
            RequestParams.empty());

        assertThat(response.getBody()).isEqualTo(requestResponse.responseBody(PaymentInitiationStatusResponse200Json.class));
    }

    private static Stream<Arguments> paymentTypes() {
        return Stream.of(arguments(PaymentService.PAYMENTS, PaymentProduct.SEPA_CREDIT_TRANSFERS),
            arguments(PaymentService.PAYMENTS, PaymentProduct.PAIN_001_SEPA_CREDIT_TRANSFERS));
    }
}
