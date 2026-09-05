package com.odc.plateforme_emploi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Les numéros mobiles guinéens comptent 9 chiffres et commencent par 6
 * (convention des opérateurs Orange/MTN/Cellcom), avec ou sans indicatif
 * international +224 ou 00224. Les espaces, points et tirets de saisie
 * courante sont tolérés puis ignorés avant validation.
 */
public class TelephoneValidator implements ConstraintValidator<Telephone, String> {

    private static final Pattern FORMAT_GUINEEN = Pattern.compile("^(\\+224|00224)?6\\d{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String nettoye = value.replaceAll("[\\s.\\-]", "");
        return FORMAT_GUINEEN.matcher(nettoye).matches();
    }
}
