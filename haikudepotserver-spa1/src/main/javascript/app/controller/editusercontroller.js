/*
 * Copyright 2014-2025, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This controller allows the user to update the details of a user within the system.</p>
 */

angular.module('haikudepotserver').controller(
    'EditUserController',
    [
        '$scope','$log','$location','$routeParams',
        'remoteProcedureCall','constants','breadcrumbs','breadcrumbFactory','userState','errorHandling',
        function(
            $scope,$log,$location,$routeParams,
            remoteProcedureCall,constants,breadcrumbs,breadcrumbFactory,userState,errorHandling) {

            $scope.workingUser = undefined;
            var amSaving = false;

            $scope.shouldSpin = function() {
                return undefined === $scope.workingUser || amSaving;
            };

            $scope.deriveFormControlsContainerClasses = function(name) {
                return $scope.editUserForm[name].$invalid ? ['form-control-group-error'] : [];
            };

            function refreshBreadcrumbItems() {
                breadcrumbs.mergeCompleteStack([
                    breadcrumbFactory.createHome(),
                    breadcrumbFactory.createViewUser($scope.workingUser),
                    breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createEditUser($scope.workingUser))
                ]);
            }

            function refreshUser() {
                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USER,
                    "get-user",
                    { nickname : $routeParams.nickname }
                ).then(
                    function (fetchedUser) {

                        $log.info('fetched user; '+fetchedUser.nickname);

                        // choose the current language as the one for editing.

                        $scope.workingUser = {
                            nickname : fetchedUser.nickname,
                            email : fetchedUser.email,
                            naturalLanguageCode : fetchedUser.naturalLanguageCode
                        };

                        refreshBreadcrumbItems();

                    },
                    errorHandling.handleRemoteProcedureCallError
                );
            }

            refreshUser();

            $scope.goSave = function() {

                if ($scope.editUserForm.$invalid) {
                    throw Error('expected the save of a user to only to be possible if the form is valid');
                }

                amSaving = true;

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USER,
                    "update-user",
                    {
                        filter : [ 'NATURALLANGUAGE', 'EMAIL' ],
                        nickname : $scope.workingUser.nickname,
                        email : $scope.workingUser.email,
                        naturalLanguageCode : $scope.workingUser.naturalLanguageCode
                    }
                ).then(
                    function () {
                        $log.info('did update user; '+$scope.workingUser.nickname);

                        // if the currently authenticated user is the one that has just been edited then we should
                        // also update the current language.

                        if (userState.user().nickname === $scope.workingUser.nickname) {
                            userState.naturalLanguageCode($scope.workingUser.naturalLanguageCode);
                        }

                        breadcrumbs.popAndNavigate();
                    },
                    function (err) {

                        switch (err.code) {
                            case remoteProcedureCall.errorCodes.VALIDATION:
                                errorHandling.relayValidationFailuresIntoForm(
                                    err.data, $scope.editUserForm);
                                break;

                            default:
                                errorHandling.handleRemoteProcedureCallError(err);
                                break;
                        }

                        amSaving = false;
                    }
                );

            }

        }
    ]
);
