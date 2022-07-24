/*
 * Copyright 2014-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

angular.module('haikudepotserver').controller(
    'ViewUserRatingController',
    [
        '$scope', '$log', '$location', '$routeParams',
        'remoteProcedureCall', 'constants', 'errorHandling', 'breadcrumbs',
        'breadcrumbFactory', 'userState',
        function(
            $scope, $log, $location, $routeParams,
            remoteProcedureCall, constants, errorHandling, breadcrumbs,
            breadcrumbFactory, userState) {

            var amUpdating = false;
            $scope.breadcrumbItems = undefined;
            $scope.userRating = undefined;
            $scope.canEdit = undefined;
            $scope.toRemoveUserRating = false;

            $scope.shouldSpin = function () {
                return undefined === $scope.userRating || amUpdating;
            };

            $scope.hasRating = function () {
                return $scope.userRating &&
                    angular.isNumber($scope.userRating.rating);
            };

            function refreshBreadcrumbItems() {
                breadcrumbs.mergeCompleteStack([
                    breadcrumbFactory.createHome(),
                    breadcrumbFactory.createViewPkgWithSpecificVersionFromPkgVersion($scope.userRating.pkgVersion),
                    breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createViewUserRating($scope.userRating))
                ]);
            }

            /**
             * <P>This function will recheck to see if the user is able to edit the user rating or
             * not.  It stores this in the scope and then elements can show/hide depending on the
             * state of the flag.</P>
             */

            function refreshAuthorization() {
                $scope.canEdit = undefined;

                userState.areAuthorized([
                    {
                        targetType: 'USERRATING',
                        targetIdentifier: $scope.userRating.code,
                        permissionCode: 'USERRATING_EDIT'
                    }
                ]).then(
                    function (flag) {
                        $scope.canEdit = flag;
                    },
                    function () {
                        throw Error('unable to establish if the user is able to edit the user rating');
                    }
                );
            }

            function refreshUserRating() {
                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USERRATING,
                    "get-user-rating",
                    { code : $routeParams.code }
                ).then(
                    function (userRatingData) {
                        $scope.userRating = userRatingData;
                        refreshBreadcrumbItems();
                        refreshAuthorization();
                        $log.info('fetched user rating; '+userRatingData.code);
                    },
                    function (err) {
                        errorHandling.handleRemoteProcedureCallError(err);
                    }
                );
            }

            refreshUserRating();

            // --------------------------------
            // ACTIONS

            $scope.canDeactivate = function () {
                return $scope.userRating && $scope.userRating.active;
            };

            $scope.canReactivate = function () {
                return $scope.userRating && !$scope.userRating.active;
            };

            $scope.goReactivate = function () {
                setActive(true);
            };

            $scope.goDeactivate = function () {
                setActive(false);
            };

            $scope.goRemove = function () {
                $scope.toRemoveUserRating = true;
            };

            $scope.goCancelRemove = function () {
                $scope.toRemoveUserRating = false;
            };

            $scope.goConfirmRemove = function () {

                amUpdating = true;

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USERRATING,
                    "remove-user-rating",
                    { code : $scope.userRating.code }
                ).then(
                    function () {
                        $log.info('did remove the user rating');
                        amUpdating = false;
                        $scope.toRemoveUserRating = false;
                        breadcrumbs.popAndNavigate();
                    },
                    function (err) {
                        $log.info('an error arose removing the user rating');
                        errorHandling.handleRemoteProcedureCallError(err);
                    }
                );
            };

            /**
             * <P>This function will configure the user rating to be either active or inactive.</p>
             */

            function setActive(flag) {

                amUpdating = true;

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USERRATING,
                    "update-user-rating",
                    {
                        code : $scope.userRating.code,
                        filter : [ 'ACTIVE' ],
                        active : !!flag
                    }
                ).then(
                    function () {
                        $log.info('did update the active flag on the user rating to; ' + flag);
                        $scope.userRating.active = flag;
                        amUpdating = false;
                    },
                    function (err) {
                        $log.info('an error arose updating the active flag on the user rating');
                        errorHandling.handleRemoteProcedureCallError(err);
                    }
                );

            }

        }
    ]
);
