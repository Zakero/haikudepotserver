/*
* Copyright 2014-2021, Andrew Lindesay
* Distributed under the terms of the MIT License.
*/

package org.haiku.haikudepotserver.api1;

import com.googlecode.jsonrpc4j.JsonRpcService;
import org.haiku.haikudepotserver.api1.model.userrating.*;
import org.haiku.haikudepotserver.api1.support.ObjectNotFoundException;

/**
 * <p>This API interface covers all aspects of user ratings of packages.</p>
 */

@JsonRpcService("/__api/v1/userrating")
public interface UserRatingApi {

    /**
     * <p>This method will re-calculate and store the user rating for the nominated packaging.</p>
     */

    DeriveAndStoreUserRatingForPkgResult deriveAndStoreUserRatingForPkg(DeriveAndStoreUserRatingForPkgRequest request);

    /**
     * <p>This method will trigger the re-calculation of user ratings for all of the packages in the system.</p>
     */

    DeriveAndStoreUserRatingsForAllPkgsResult deriveAndStoreUserRatingsForAllPkgs(DeriveAndStoreUserRatingsForAllPkgsResult request);

    /**
     * <p>This will find the user rating identified by the supplied code and will return data pertaining to that
     * or if the user rating was not able to be found for the code supplied then it will throw an instance of
     * {@link ObjectNotFoundException}.  Note that this invocation
     * has no authorization on it; it is effectively public.</p>
     */

    GetUserRatingResult getUserRating(GetUserRatingRequest request);

    /**
     * <p>This will find the user rating identified by the user and the package version.  If not such user rating
     * exists then this method will throws an instance of
     * {@link ObjectNotFoundException}.  Note that there is no
     * authorization on it; it is effectively public.</p>
     */

    GetUserRatingByUserAndPkgVersionResult getUserRatingByUserAndPkgVersion(GetUserRatingByUserAndPkgVersionRequest request);

    /**
     * <p>This method will create a user rating based on the data provided.  In the result is a code that
     * identifies this rating.</p>
     */

    CreateUserRatingResult createUserRating(CreateUserRatingRequest request);

    /**
     * <p>This method will update the user rating.  The user rating is identified by the supplied code and the
     * supplied filter describes those properties of the user rating that should be updated.</p>
     */

    UpdateUserRatingResult updateUserRating(UpdateUserRatingRequest request);

    /**
     * <p>This method will return user rating search results based on the criteria supplied in the request.</p>
     */

    SearchUserRatingsResult searchUserRatings(SearchUserRatingsRequest request);

    /**
     * <p>This method will delete a user rating so that it is no longer stored.</p>
     */

    RemoveUserRatingResult removeUserRating(RemoveUserRatingRequest request);

}
