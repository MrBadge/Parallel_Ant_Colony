$body = $("body");

$(document).on({
    ajaxStart: function () {
        $body.addClass("loading");
    },
    ajaxStop: function () {
        $body.removeClass("loading");
    }
});

function loadExtScript(src, test, callback) {
    var s = document.createElement('script');
    s.src = src;
    document.body.appendChild(s);

    var callbackTimer = setInterval(function () {
        var call = false;
        try {
            call = test.call();
        } catch (e) {
        }

        if (call) {
            clearInterval(callbackTimer);
            callback.call();
        }
    }, 100);
}

// loadExtScript('https://maps.googleapis.com/maps/api/js?key=AIzaSyA20nUnH7noBCwMAFaG150mheOakPqngVg&signed_in=true&callback=initMap', function () {
//     loadExtScript('https://cdn.rawgit.com/printercu/google-maps-utility-library-v3-read-only/master/markerwithlabel/src/markerwithlabel.js')
// });

var colourArray = ['navy', 'grey', 'fuchsia', 'black', 'lime', 'maroon', 'purple', 'aqua', 'red', 'green', 'silver', 'olive', 'blue', 'yellow', 'teal', 'orange', 'crimson', 'tomato', 'brown'],
    markers = [],
    renderArray = [],
    total_result;

function initMap() {
    directionsService = new google.maps.DirectionsService;
    directionsDisplay = new google.maps.DirectionsRenderer;
    map = new google.maps.Map(document.getElementById('map'), {
        zoom: 10,
        center: {lat: 54.308918, lng: 48.395865}
    });
    // directionsDisplay.setMap(map);

    // google.maps.event.addDomListener(window, 'load', getLocation());
}

function getLocation() {

    {
        if (navigator.geolocation) {

            var options = {
                enableHighAccuracy: true,
                timeout: 5000,
                maximumAge: 0
            };

            navigator.geolocation.getCurrentPosition(success, error, options);
        }

        else {
            x.innerHTML = "Geolocation is not supported by this browser.";
        }
    }

}

function error(e) {

    console.log("error code:" + e.code + '; message: ' + e.message);

}

function success(position) {
    var lat = position.coords.latitude;
    var lng = position.coords.longitude;

    var myLocation = new google.maps.LatLng(lat, lng);


    var mapOptions = {
        center: new google.maps.LatLng(myLocation.lat(), myLocation.lng()),
        zoom: 10,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    map = new google.maps.Map(document.getElementById("map"), mapOptions);
}

function getCookie(name) {
    var cookieValue = null;
    if (document.cookie && document.cookie != '') {
        var cookies = document.cookie.split(';');
        for (var i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i]);
            if (cookie.substring(0, name.length + 1) == (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

function handleFileSelect() {
    if (!window.File || !window.FileReader || !window.FileList || !window.Blob) {
        alert('The File APIs are not fully supported in this browser.');
        return;
    }

    var input = document.getElementById('fileinput');
    if (!input) {
        alert("Um, couldn't find the fileinput element.");
    }
    else if (!input.files) {
        alert("This browser doesn't seem to support the `files` property of file inputs.");
    }
    else if (!input.files[0]) {
        alert("Please select a file before clicking 'Load'");
    }
    else {
        file = input.files[0];
        fr = new FileReader();
        fr.onload = handleContents;
        fr.readAsText(file);
    }
}

function draw_directions(start, end) {
    directionsService.route({
        origin: start,
        destination: end,
        travelMode: google.maps.TravelMode.DRIVING
    }, function (response, status) {
        if (status === google.maps.DirectionsStatus.OK) {
            directionsDisplay.setDirections(response);
        } else {
            window.alert('Directions request failed due to ' + status);
        }
    });
}

function handleContents() {
    $.ajax({
        dataType: 'json',
        method: 'POST',
        url: '/set_task/',
        data: {
            'csrfmiddlewaretoken': getCookie('csrftoken'),
            'data': fr.result
        },
        success: function (answer) {
            if (!answer) {
                console.log('Some weird server error!');
                return
            }
            // alert('Stats:\n\nClients visited: ' + answer[0].totals.clients_visited + '\nTotal distance: ' + answer[0].totals.distance / 1000 + 'км\n');
            total_result = answer;
            if($("#without_routes").attr("checked") != 'checked') {
                displayRoutes(answer);
            }

            for (var k = 0; k < markers.length; ++k) {
                markers[k].setMap(null);
            }

            var raw_result = total_result[0],
                depot = raw_result.routes[0][0];

            createMarker(new google.maps.LatLng(depot.lat, depot.lng), map, 'D', 'red',
                formatDepotInfo(depot.address, raw_result.totals));

            for (var p = 0; p < raw_result.routes.length; ++p) {
                for (var k = 1; k < raw_result.routes[p].length; ++k) {
                    var point = raw_result.routes[p][k];
                    createMarker(new google.maps.LatLng(point.lat, point.lng), map, k + '', colourArray[p],
                        formatPlaceInfo(point));
                }
            }

        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            console.log("Status: " + textStatus);
            console.log("Error: " + errorThrown);
        }
    });
}

function displayRoutes(solution) {

    var requestArray = [];

    for (var i = 0; i < solution.length; ++i) {
        var sdvrp_solution = solution[i];
        for (var j = 0; j < sdvrp_solution['routes'].length; j++) {
            var sub_route = sdvrp_solution['routes'][j],
                waypts = [],
                start = sub_route[0].address,
                finish = start;

            for (var k = 1; k < sub_route.length; k++) {
                waypts.push({
                    location: sub_route[k].address,
                    stopover: true
                });
                // draw_directions(sub_route[i], sub_route[i + 1])
            }
            var request = {
                origin: start,
                destination: finish,
                waypoints: waypts,
                optimizeWaypoints: false,
                travelMode: google.maps.TravelMode.DRIVING
            };

            requestArray.push({"route": sub_route, "request": request, "totals": sdvrp_solution["totals"]});
        }
    }

    processRequests(requestArray);
}

function pinSymbol(color) {
    return {
        path: 'M 0,0 C -2,-20 -10,-22 -10,-30 A 10,10 0 1,1 10,-30 C 10,-22 2,-20 0,0 z',
        fillColor: color,
        fillOpacity: 1,
        strokeColor: '#000',
        strokeWeight: 2,
        scale: 2
    };
}

function createMarker(location, map, label, color, info_text) {
    var marker = new MarkerWithLabel({
        position: location,
        map: map,
        // draggable: true,
        // raiseOnDrag: true,
        labelContent: label,
        labelAnchor: new google.maps.Point(15, 65),
        labelClass: "labels",
        labelInBackground: false,
        icon: pinSymbol(color)
    });

    google.maps.event.addListener(marker, 'click', getInfoCallback(map, info_text));
    markers.push(marker);
}

function secondsToString(seconds) {
    // var numyears = Math.floor(seconds / 31536000);
    // var numdays = Math.floor((seconds % 31536000) / 86400);
    var numhours = Math.floor(((seconds % 31536000) % 86400) / 3600);
    var numminutes = Math.floor((((seconds % 31536000) % 86400) % 3600) / 60);
    var numseconds = (((seconds % 31536000) % 86400) % 3600) % 60;
    if (numhours > 0)
        return numhours + " ч " + numminutes + " м " + numseconds + " с";
    else
        return numminutes + " м " + numseconds + " с";

}

function formatPlaceInfo(info) {
    return 'Адрес: ' + info.address +
        '<br> Потребность в товаре: ' + info.demand + ' ед' +
        '<br> Время выезда с начала маршрута: ' + secondsToString(info.at) +
        '<br> Время обслуживания: ' + info.st + ' c' +
        '<br> Начало окна: ' + secondsToString(info.tis) +
        '<br> Конец окна: ' + secondsToString(info.tie)
}

function getInfoCallback(map, content) {
    var infowindow = new google.maps.InfoWindow({content: content});
    return function () {
        infowindow.setContent(content);
        infowindow.open(map, this);
    };
}

function formatDepotInfo(address, totals) {
    return 'Адрес: ' + address +
        '<br> Клиентов посещено: ' + totals.clients_visited + '/' + totals.clients_total +
        '<br> Суммарное расстояние: ' + totals.distance / 1000. + ' км' +
        '<br> Количество маршрутов: ' + totals.routes +
        '<br> Времени затрачено на поиск решения: ' + totals.time + ' c'
}

function processRequests(requestArray) {
    var i = 0;

    for (var k = 0; k < renderArray.length; ++k) {
        renderArray[k].setMap(null);
    }

    for (var k = 0; k < markers.length; ++k) {
        markers[k].setMap(null);
    }

    function submitRequest() {
        directionsService.route(requestArray[i].request, directionResults);
    }

    function directionResults(result, status) {
        if (status == google.maps.DirectionsStatus.OK) {
            renderArray[i] = new google.maps.DirectionsRenderer();
            renderArray[i].setMap(map);

            renderArray[i].setOptions({
                preserveViewport: true,
                suppressMarkers: true,
                // suppressInfoWindows: true,
                polylineOptions: {
                    strokeWeight: 4,
                    strokeOpacity: 0.8,
                    strokeColor: colourArray[i]
                }
            });

            // createMarker(result.routes[0].legs[0].start_location.toJSON(), map, 'D', 'red',
            //     formatDepotInfo(result.routes[0].legs[0].start_address, requestArray[i].totals));
            //
            // for (var j = 1; j < result.routes[0].legs.length; ++j) {
            //     // var info = {at: '', st: '', address: '', demand: ''};
            //     // for (var l = 0; l < requestArray[i].route.length; ++l) {
            //     //     if (result.routes[0].legs[j].start_address == requestArray[i].route[l].address) {
            //     //         info = requestArray[i].route[l];
            //     //         break;
            //     //     }
            //     // }
            //     createMarker(result.routes[0].legs[j].start_location.toJSON(), map, j + '', colourArray[i],
            //         formatPlaceInfo(requestArray[i].route[j]));
            // }

            renderArray[i].setDirections(result);
            nextRequest();
        } else {
            // alert('Could not draw route: ' + status);
            console.log('Could not draw route: ' + status);
        }

    }

    function nextRequest() {
        i++;
        if (i >= requestArray.length) {
            return;
        }
        submitRequest();
    }

    submitRequest();
}