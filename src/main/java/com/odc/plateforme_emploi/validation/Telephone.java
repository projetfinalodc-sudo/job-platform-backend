package com.odc.plateforme_emploi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valide qu'un numéro de téléphone respecte le format guinéen (9 chiffres
 * commençant par 6, avec ou sans indicatif +224). Champ optionnel : une
 * valeur vide est considérée valide, combiner avec @NotBlank si obligatoire.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TelephoneValidator.class)
public @interface Telephone {

    String message() default "Numéro de téléphone invalide (format attendu : +224 6XX XX XX XX)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
