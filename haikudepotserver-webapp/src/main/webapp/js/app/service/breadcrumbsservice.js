/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This service helps in the pushing of breadcrumb items, but also maintains the state of the breadcrumb trail.  An
 * item should have the following properties;</p>
 *
 * <ul>
 *     <li>title -  verbatim string to use as the title of the breadcrumb item.</li>
 *     <li>titleKey - a key into the localization for the title of the breadcrumb item.</li>
 *     <li>titleParameters - an array of objects to replace tokens in the localized title.</li>
 *     <li>path - the path to visit; set into the $location.</li>
 *     <li>search - search data to configure on the $location.</li>
 * </ul>
 */

angular.module('haikudepotserver').factory('breadcrumbs',
    [
        '$rootScope','$location','$log',
        function($rootScope,$location,$log) {

            /**
             * <p>This variable contains the breadcrumb trail.  Inherently, "home" is not on the breadcrumb trail
             * because it is always there.</p>
             * @type {undefined}
             */

            var stack = undefined;

            function verifyItem(item) {

                if(!item) {
                    throw Error('a breadcrumb item was expected');
                }

                if(!item.titleKey || !item.titleKey.length) {
                    throw Error('a breadcrumb item must have a title key');
                }

                if(!item.path || !item.path.length) {
                    throw Error('a breadcrumb item must have a path');
                }

                if(!item.search || !item.search.bcguid) {
                    throw Error('a "bcguid" is expected on a breadcrumb item');
                }
            }

            function verifyItems(items) {

                if(!items) {
                    throw Error('breadcrumb items were expected');
                }

                _.each(items, function(item) {
                    verifyItem(item);
                });
            }

            /**
             * <p>This function will return the breadcrumbs to its pristine state.</p>
             */

            function reset(stackIn) {
                $log.debug('reset the breadcrumb stack');

                stack = undefined;

                if(stackIn) {
                    verifyItems(stackIn);
                    stack = stackIn;
                }

                $rootScope.$broadcast('breadcrumbChangeSuccess',stack);
            }

            /**
             * <p>This function returns the item that is at the top-most of the stack.</p>
             */

            function peek() {
                if(stack&&stack.length) {
                    return stack[stack.length-1];
                }

                return null;
            }

            /**
             * <p>This will push an item to the breadcrumbs.  It is possible to pass null, in which case the
             * item will not be pushed, but the breadcrumbs will, nonetheless, be initialized and will no
             * longer be pristine.</p>
             */

            function push(item) {

                verifyItem(item);

                if(!stack) {
                    stack = [];
                }

                stack.push(item);

                $rootScope.$broadcast('breadcrumbChangeSuccess',stack);
            }

            /**
             * <p>This function will remove the deepest item from the breadcrumb stack.</p>
             */

            function pop() {
                if(!stack||!stack.length) {
                    throw Error('attempt to pop from empty breadcrumb stack');
                }

                var item = stack.pop();
                $rootScope.$broadcast('breadcrumbChangeSuccess',stack);
                return item;
            }

            /**
             * <p>This function will pop items from the stack until it hits the one mentioned and will then leave
             * that item on the stack.</p>
             */

            function popTo(item) {
                if(!stack) {
                    throw Error('the breadcrumb stack is empty; not possible to popTo(..)');
                }

                while(stack.length && stack[stack.length-1] != item) {
                    stack.pop();
                }

                if(!stack.length) {
                    throw Error('the item requested to popTo was not found on the breadcrumb stack');
                }

                $rootScope.$broadcast('breadcrumbChangeSuccess',stack);

                return stack[stack.length-1];
            }

            /**
             * <p>This function will get the data from the inbound stack and merge it into the existing breadcrumb
             * stack.  It can do this in a couple of different ways;</p>
             *
             * <ul>
             *     <li>
             *         If the stack is not empty and the last item is not already at the end of the stack, then it
             *         will take the last item in the inbound stack and will put it at the end of the stack.
             *     </li>
             *     <li>
             *         If the stack is not empty and the last item in the inbound stack is the same as the one in the
             *         existing stack, then it will merge the data from the inbound stack's top most item into the data
             *         of the existing stack.
             *     </li>
             *     <li>
             *         If the stack is empty (or undefined) then it will just use the inbound stack.
             *     </li>
             *  <ul>
             *
             *  <p>Items are considered to be the same if their location matches.</p>
             */

            function mergeCompleteStack(stackIn) {
                if(!stackIn || !stackIn.length) {
                    throw Error('attempt to merge an empty stack into the existing stack; not possible');
                }

                verifyItems(stackIn);

                if(!stack || !stack.length) {
                    stack = stackIn;
                }
                else {

                    // if the inbound top-of-stack is an existing item that is already on the stack
                    // then we should pop down to that item.

                    var peekStackIn = stackIn[stackIn.length - 1];
                    var matchingStackItem = undefined;

                    if(peekStackIn.search.bcguid) {
                        matchingStackItem = _.find(stack, function(s) { return s.search.bcguid == peekStackIn.search.bcguid; });
                    }

                    if(matchingStackItem) {
                        popTo(matchingStackItem);
                    }
                    else
                    {
                        // if the last item is not the same path, then stick that item at the top
                        // of the stack.

                        if (peek().path != peekStackIn.path) {
                            stack.push(stackIn[stackIn.length - 1]);
                        }
                    }
                }

                // the breadcrumb being merged in may have more search items than are presently in the
                // location - in this case those additional search items need to be merged in.

                if(peek().search && $location.path() == peek().path) {
                    _.each(peek().search, function(value,key) {
                        $location.search(key,value);
                    });
                }

                $rootScope.$broadcast('breadcrumbChangeSuccess',stack);
            }

            /**
             * <p>This function will set the currently viewed page to be the one described in the 'item'.</p>
             */

            function navigateTo(item) {
                if(!item) {
                    $location.path('/').search({});
                }
                else {
                    $location.path(item.path);

                    if(item.search) {
                        $location.search(item.search);
                    }
                    else {
                        $location.search({});
                    }
                }
            }

            return {

                stack : function() {
                    return stack;
                },

                reset : function(stackIn) {
                    reset(stackIn);
                    return stack;
                },

                resetAndNavigate : function(stackIn) {
                    reset(stackIn);
                    navigateTo(peek());
                    return stack;
                },

                pop : function() {
                    return pop();
                },

                /**
                 * <p>Pop the next item off the stack and navigate to the next one on the stack.  Returns the next
                 * one on the stack.</p>
                 */

                popAndNavigate: function() {
                    pop();

                    if(!stack.length) {
                        throw Error('have popped from the stack, but there is now nothing to navigate to');
                    }

                    var top = peek();
                    navigateTo(top);
                    return top;
                },

                peek : function() {
                    return peek();
                },

                /**
                 * <p>This will pop off items from the stack until it reaches this item.  It will then navigate
                 * to the item and will return that item.</p>
                 */

                popToAndNavigate: function(item) {
                    var result = popTo(item);

                    if(!result) {
                        throw Error('unable to find the item on the stack');
                    }

                    navigateTo(result);

                    return result;
                },

                /**
                 * <p>This will look to see what is being supplied and compare it with what it has on the stack at
                 * the moment.  It may be possible to just merge the inbound stack in or if the stack is presently
                 * empty then it will just use the inbound stack.</p>
                 */

                mergeCompleteStack : function(stackIn) {
                    mergeCompleteStack(stackIn);
                    return stack;
                },

                pushAndNavigate : function(item) {
                    verifyItem(item);
                    push(item);
                    navigateTo(item);
                    return item;
                }

            };

        }
    ]
);
