var AuthController = angular.module('authController',[])

AuthController.controller('AuthCtrl', [
   	'$rootScope',
	'$scope',
	'$http',
	'$location',
	function AuthCtrl($rootScope, $scope, $http,$location) {
   		$scope.auth = $rootScope.auth;
		$scope.loginUser = function() {
			
			if($scope.username == "Peter" && $scope.password == "12345"){
				$scope.username = '';
				$scope.password = '';
	   			$rootScope.auth = true;
	   			$rootScope.user = "Peter";
	   			$location.path('/clients/');
			}
			else {
				$scope.error = "Invalid username or password:" + $scope.username;
	            //console.error($scope.error);
			}
		}
		
		$scope.$on('$routeChangeSuccess', function ($rootScope, $location) {
			  if ($rootScope.auth) $location.path('/clients/');
			});
   	}	   	
]);


