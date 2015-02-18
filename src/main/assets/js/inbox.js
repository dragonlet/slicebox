(function () {
   'use strict';
}());

angular.module('slicebox.inbox', ['ngRoute'])

.config(function($routeProvider) {
  $routeProvider.when('/inbox', {
    templateUrl: '/assets/partials/inbox.html',
    controller: 'InboxCtrl'
  });
})

.controller('InboxCtrl', function($scope, $http, $modal, $q, $interval) {
    // Initialization
    // $scope.objectActions =
    //     [
    //         {
    //             name: 'Remove',
    //             action: removeBoxes
    //         }
    //     ];

    $scope.callbacks = {};

    $scope.uiState = {
        errorMessage: null
    };

    var timer = $interval(function() {
        if (angular.isDefined($scope.callbacks.inboxTable)) {
            $scope.callbacks.inboxTable.reloadPage();
        }
    }, 1000);

    $scope.$on('$destroy', function() {
        $interval.cancel(timer);
    });
  
    // Scope functions
    $scope.loadInboxPage = function(startIndex, count, orderByProperty, orderByDirection) {
        return $http.get('/api/inbox');
    };
});