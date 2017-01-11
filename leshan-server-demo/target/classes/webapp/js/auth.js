var AuthController = angular.module('authController',[])

AuthController.controller('AuthCtrl', [
   	'$rootScope',
	'$scope',
	'$http',
	'$location',
	function AuthCtrl($rootScope, $scope, $http,$location) {

		$scope.loginUser = function() {


			$http.post('api/broker/login/'+$scope.username, "login:"+$scope.password). error(function(data, status, headers, config){
	            $scope.error = "Error: " + status + " " + data;
	            console.error($scope.error);
	        }).success(function(data, status, headers, config) {
	        	$scope.error = "Data:" + data;
        		if(data === 'correctLogin')
    			{
        			$rootScope.auth = true;
    	   			$rootScope.user = $scope.username;
//    	   			$location.path('/lights/');
//    	   			$rootScope.homedir = '#/lights/';
    	   			$location.path('/clients/');
    	   			$rootScope.homedir = '#/clients/';
    			}
        		else if (data === 'notAtDesk'){
    				$scope.error = "You are currently not at your desk.";
        		}
        		else {
    				$scope.error = "Invalid username or password";
    	            //console.error($scope.error);
    			}

	        });
		}
   	}
]);
