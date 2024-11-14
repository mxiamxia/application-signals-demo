// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package org.springframework.samples.petclinic.api.boundary.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.samples.petclinic.api.application.*;
import org.springframework.samples.petclinic.api.dto.*;
import org.springframework.samples.petclinic.api.utils.WellKnownAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/")
public class ApiController {

    private final CustomersServiceClient customersServiceClient;
    private final VetsServiceClient vetsServiceClient;
    private final VisitsServiceClient visitsServiceClient;
    private final InsuranceServiceClient insuranceServiceClient;
    private final BillingServiceClient billingServiceClient;
    private final PaymentClient paymentClient;
    private final NutritionServiceClient nutritionServiceClient;

    @GetMapping(value = "customer/owners")
    public Flux<OwnerDetails> getOwners() {
        return customersServiceClient.getOwners();
    }

    @WithSpan
    @GetMapping(value = "customer/owners/{ownerId}")
    public Mono<OwnerDetails> getOwner(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId) {
        return customersServiceClient.getOwner(ownerId);
    }

    @WithSpan
    @PutMapping(value = "customer/owners/{ownerId}")
    public Mono<Void> getOwner(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @RequestBody OwnerRequest ownerRequest) {
        return customersServiceClient.updateOwner(ownerId, ownerRequest);
    }

    @PostMapping(value = "customer/owners")
    public Mono<Void> addOwner(@RequestBody OwnerRequest ownerRequest) {
        return customersServiceClient.addOwner(ownerRequest);
    }

    @GetMapping(value = "customer/petTypes")
    public Flux<PetType> getPetTypes() {
        return customersServiceClient.getPetTypes();
    }

    @WithSpan
    @GetMapping(value = "customer/owners/{ownerId}/pets/{petId}")
    public Mono<PetFull> getPetTypes(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId) {
        return customersServiceClient.getPet(ownerId, petId);
    }

    @WithSpan
    @GetMapping(value = "customer/diagnose/owners/{ownerId}/pets/{petId}")
    public Mono<Void> diagnosePet(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId) {
        log.info("DEBUG: Inside the diagnose API - diagnosePet");
        return customersServiceClient.diagnosePet(ownerId, petId);
    }

    @WithSpan
    @PutMapping("customer/owners/{ownerId}/pets/{petId}")
    public Mono<Void> updatePet(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId,
            @RequestBody PetRequest petRequest) {
        return customersServiceClient.updatePet(ownerId, petId, petRequest);
    }

    @WithSpan
    @PostMapping("customer/owners/{ownerId}/pets")
    public Mono<PetFull> addPet(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @RequestBody PetRequest petRequest) {
        return customersServiceClient.addPet(ownerId, petRequest);
    }

    @GetMapping(value = "vet/vets")
    public Flux<VetDetails> getVets() {
        return vetsServiceClient.getVets();
    }

    @WithSpan
    @GetMapping(value = "visit/owners/{ownerId}/pets/{petId}/visits")
    public Mono<Visits> getVisits(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId) {
        return visitsServiceClient.getVisitsForOwnersPets(ownerId, petId);
    }

    @WithSpan
    @PostMapping(value = "visit/owners/{ownerId}/pets/{petId}/visits")
    public Mono<String> addVisit(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId,
            final @RequestBody VisitDetails visitDetails) {
        return visitsServiceClient.addVisitForOwnersPets(ownerId, petId, visitDetails);
    }

    @GetMapping(value = "insurance/insurances")
    public Flux<InsuranceDetail> getInsurance() {
        return insuranceServiceClient.getInsurances();
    }

    @GetMapping(value = "billing/billings")
    public Flux<BillingDetail> getBillings() {
        return billingServiceClient.getBillings();
    }

    @PostMapping(value = "insurance/pet-insurances")
    public Mono<Void> addPetInsurance(final @RequestBody PetInsurance petInsurance) {
        System.out.println(petInsurance.toString());
        return insuranceServiceClient.addPetInsurance(petInsurance);
    }

    @WithSpan
    @PutMapping(value = "insurance/pet-insurances/{petId}")
    public Mono<PetInsurance> updatePetInsurance(@SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId,
            final @RequestBody PetInsurance petInsurance) {
        return insuranceServiceClient.updatePetInsurance(petId, petInsurance);
    }

    @WithSpan
    @GetMapping(value = "insurance/pet-insurances/{petId}")
    public Mono<PetInsurance> getPetInsurance(@SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId) {
        return insuranceServiceClient.getPetInsurance(petId);
    }

    @WithSpan
    @GetMapping(value = "payments/owners/{ownerId}/pets/{petId}")
    public Flux<PaymentDetail> getPayments(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId) {
        return paymentClient.getPayments(ownerId, petId);
    }

    @WithSpan
    @GetMapping(value = "payments/owners/{ownerId}/pets/{petId}/{paymentId}")
    public Mono<PaymentDetail> getPaymentById(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId,
            @SpanAttribute(WellKnownAttributes.ORDER_ID) final @PathVariable String paymentId) {
        return paymentClient.getPaymentById(ownerId, petId, paymentId);
    }

    @WithSpan
    @PostMapping(value = "payments/owners/{ownerId}/pets/{petId}")
    public Mono<PaymentDetail> addPayment(@SpanAttribute(WellKnownAttributes.OWNER_ID) final @PathVariable int ownerId, @SpanAttribute(WellKnownAttributes.PET_ID) final @PathVariable int petId,
            final @RequestBody PaymentAdd paymentAdd) {
        return paymentClient.addPayment(ownerId, petId, paymentAdd);
    }

    @DeleteMapping(value = "payments/clean-db")
    public Mono<PaymentDetail> cleanPaymentTable() {
        return paymentClient.cleanPaymentTable();
    }

    @GetMapping(value = "nutrition/facts/{petType}")
    public Mono<PetNutrition> getNutrition(final @PathVariable String petType) {
        return nutritionServiceClient.getPetNutrition(petType);
    }

}
