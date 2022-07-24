/*
 * Copyright 2014-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

angular.module('haikudepotserver').controller(
    'AddEditRepositoryController',
    [
        '$scope','$log','$routeParams',
        'remoteProcedureCall','constants','breadcrumbs','breadcrumbFactory','userState','errorHandling','referenceData',
        function(
            $scope,$log,$routeParams,
            remoteProcedureCall,constants,breadcrumbs,breadcrumbFactory,userState,errorHandling,referenceData) {

            $scope.workingRepository = undefined;
            $scope.amEditing = !!$routeParams.code;
            var amSaving = false;

            $scope.shouldSpin = function() {
                return !$scope.workingRepository || amSaving;
            };

            $scope.deriveFormControlsContainerClasses = function(name) {
                return $scope.addEditRepositoryForm[name].$invalid ? ['form-control-group-error'] : [];
            };

            // the validity of this field may have been set to false because of the
            // attempt to save it on the server-side.  This function will be hit when
            // the code changes so that validation does not apply.

            $scope.codeChanged = function() {
                $scope.addEditRepositoryForm.code.$setValidity('unique',true);
            };

            $scope.informationalUrlChanged = function() {
                $scope.addEditRepositoryForm.informationUrl.$setValidity('malformed',true);
            };

            function refreshBreadcrumbItems() {

                var b = [
                    breadcrumbFactory.createHome(),
                    breadcrumbFactory.createListRepositories()
                ];

                if ($scope.amEditing) {
                    b.push(breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createEditRepository($scope.workingRepository)));
                }
                else {
                    b.push(breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createAddRepository()));
                }

                breadcrumbs.mergeCompleteStack(b);
            }

            function refreshRepository() {
                if ($routeParams.code) {
                    remoteProcedureCall.call(
                        constants.ENDPOINT_API_V2_REPOSITORY,
                        "get-repository",
                        { code : $routeParams.code }
                    ).then(
                        function (result) {
                            $scope.workingRepository = _.extend(
                                _.clone(result), { 'changePassword': false });
                            refreshBreadcrumbItems();
                            $log.info('fetched repository; '+result.code);
                        },
                        function (err) {
                            errorHandling.handleRemoteProcedureCallError(err);
                        }
                    );
                }
                else {
                    $scope.workingRepository = {};
                    refreshBreadcrumbItems();
                }
            }

            function refreshData() {
                refreshRepository();
            }

            refreshData();

            $scope.goSave = function() {

                if ($scope.addEditRepositoryForm.$invalid) {
                    throw Error('expected the save of a repository to only to be possible if the form is valid');
                }

                amSaving = true;

                if ($scope.amEditing) {

                    var filter = [ 'INFORMATIONURL', 'NAME' ];

                    if ($scope.workingRepository.changePassword) {
                        filter.push('PASSWORD');
                    }

                    remoteProcedureCall.call(
                            constants.ENDPOINT_API_V2_REPOSITORY,
                            "update-repository",
                            {
                                filter : filter,
                                informationUrl : $scope.workingRepository.informationUrl,
                                name : $scope.workingRepository.name,
                                code : $scope.workingRepository.code,
                                passwordClear : $scope.workingRepository.passwordClear
                            }
                        ).then(
                        function () {
                            $log.info('did update repository; '+$scope.workingRepository.code);
                            breadcrumbs.popAndNavigate();
                        },
                        function (err) {

                            switch (err.code) {
                                case remoteProcedureCall.errorCodes.VALIDATION:
                                    errorHandling.relayValidationFailuresIntoForm(
                                        err.data, $scope.addEditRepositoryForm);
                                    break;

                                default:
                                    errorHandling.handleRemoteProcedureCallError(err);
                                    break;
                            }

                            amSaving = false;
                        }
                    );
                }
                else {
                    remoteProcedureCall.call(
                            constants.ENDPOINT_API_V2_REPOSITORY,
                            "create-repository",
                            {
                                informationUrl : $scope.workingRepository.informationUrl,
                                name : $scope.workingRepository.name,
                                code : $scope.workingRepository.code
                            }
                        ).then(
                        function () {
                            $log.info('did create repository; '+$scope.workingRepository.code);
                            breadcrumbs.pop();
                            breadcrumbs.pushAndNavigate(breadcrumbFactory.createViewRepository($scope.workingRepository));
                        },
                        function (err) {

                            switch(err.code) {
                                case remoteProcedureCall.errorCodes.VALIDATION:
                                    errorHandling.relayValidationFailuresIntoForm(
                                        err.data, $scope.addEditRepositoryForm);
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

        }
    ]
);
