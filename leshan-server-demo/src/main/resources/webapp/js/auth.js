var AuthController = angular.module('authController',[])

AuthController.controller('AuthCtrl', [
   	'$rootScope',
	'$scope',
	'$http',
	'$location',
	function AuthCtrl($rootScope, $scope, $http,$location) {
   		
		$scope.loginUser = function() {
			
			
			$http.post('api/login/'+$scope.username, "password:"+$scope.password). error(function(data, status, headers, config){
	            $scope.error = "Error: " + status + " " + data;
	            console.error($scope.error);
	        }).success(function(data, status, headers, config) {
	        	$scope.error = "Data:" + data;
        		if(data === 'correctLogin')
    			{
        			$rootScope.auth = true;
    	   			$rootScope.user = "Peter";
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
