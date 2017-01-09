/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/

'use strict';

/* App Module */

var leshanApp = angular.module('leshanApp',[ 
        'ngRoute',
        'clientControllers',
        'lightControllers',
        'objectDirectives',
        'instanceDirectives',
        'resourceDirectives',
        'resourceFormDirectives',
        'lwResourcesServices',
        'securityControllers',
        'uiDialogServices',
        'modalInstanceControllers',
        'ui.bootstrap',
        'authController',
]);

leshanApp.run(function($rootScope) {
	$rootScope.auth = false;
	$rootScope.homedir = '#/login/';
});

leshanApp.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.
        when('/clients',           { templateUrl : 'partials/client-list.html',   controller : 'ClientListCtrl', publicAccess : false }).
        when('/clients/:clientId', { templateUrl : 'partials/client-detail.html', controller : 'ClientDetailCtrl', publicAccess : false }).
        when('/security',          { templateUrl : 'partials/security-list.html', controller : 'SecurityCtrl', publicAccess : false }).
        when('/lights',           { templateUrl : 'partials/client-list.html',   controller : 'LightListCtrl', publicAccess : false }).
        when('/lights/:clientId', { templateUrl : 'partials/client-detail.html', controller : 'LightDetailCtrl', publicAccess : false }).
        when('/login', 				{ templateUrl : 'partials/auth.html', controller : 'AuthCtrl', publicAccess : true }).
        otherwise({ redirectTo : '/login' });
}]);



leshanApp.run(function($rootScope, $location, $route) {

    var routesOpenToPublic = [];
    angular.forEach($route.routes, function(route, path) {
    	// push route onto routesOpenToPublic if it has a truthy publicAccess value
    	route.publicAccess && (routesOpenToPublic.push(path));
    });

    $rootScope.$on('$routeChangeStart', function(event, nextLoc, currentLoc) {
        var closedToPublic = (-1 === routesOpenToPublic.indexOf($location.path()));
        if(closedToPublic && !$rootScope.auth) {
            $location.path('/login');
        }
    });
});
