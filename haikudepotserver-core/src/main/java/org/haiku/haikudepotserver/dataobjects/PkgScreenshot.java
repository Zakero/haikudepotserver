/*
 * Copyright 2013-2018, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.dataobjects;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.haiku.haikudepotserver.dataobjects.auto._PkgScreenshot;
import org.haiku.haikudepotserver.dataobjects.support.MutableCreateAndModifyTimestamped;

import java.util.List;
import java.util.Optional;

public class PkgScreenshot extends _PkgScreenshot implements Comparable<PkgScreenshot>, MutableCreateAndModifyTimestamped {

    public static PkgScreenshot getByCode(ObjectContext context, String code) {
        return tryGetByCode(context, code)
                .orElseThrow(() -> new IllegalStateException("unable to find the screenshot with code [" + code + "]"));
    }

    public static Optional<PkgScreenshot> tryGetByCode(ObjectContext context, String code) {
        Preconditions.checkArgument(null != context, "the context must be supplied");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(code), "the code must be supplied");
        return Optional.ofNullable(ObjectSelect.query(PkgScreenshot.class).where(CODE.eq(code)).selectOne(context));
    }

    public PkgScreenshotImage getPkgScreenshotImage() {
        return tryGetPkgScreenshotImage()
                .orElseThrow(() -> new IllegalStateException("unable to find the pkg screenshot's image"));
    }

    /**
     * <p>As there should be only one of these, if there are two then this method will throw an
     * {@link IllegalStateException}.</p>
     */

    public Optional<PkgScreenshotImage> tryGetPkgScreenshotImage() {
        List<PkgScreenshotImage> images = getPkgScreenshotImages();

        return switch (images.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(images.getFirst());
            default -> throw new IllegalStateException("more than one pkg icon image found on an icon image");
        };
    }

    @Override
    public int compareTo(PkgScreenshot o) {
        return getOrdering().compareTo(o.getOrdering());
    }

    @Override
    protected void validateForSave(ValidationResult validationResult) {
        super.validateForSave(validationResult);

        if(getHeight() <= 0) {
            validationResult.addFailure(new BeanValidationFailure(this, HEIGHT.getName(), "range"));
        }

        if(getWidth() <= 0) {
            validationResult.addFailure(new BeanValidationFailure(this, WIDTH.getName(), "range"));
        }

        if(getLength() <= 0) {
            validationResult.addFailure(new BeanValidationFailure(this, LENGTH.getName(), "range"));
        }
    }

}
