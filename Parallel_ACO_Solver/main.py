import json
import time
from collections import namedtuple
from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib import parse

import logging
import pants

input_problem = 'text_problem_good.txt'
NODE = namedtuple('NODE', 'uid,address,demand,tis,tie,st,isReal,becomeRealProbability,db_uid,lat,lng')
ANT = namedtuple('ANT', 'capacity,tis,tie')
local_db = json.load(open('final_matrix.json'))


def get_db_info(addr):
    uid = local_db['addresses'].get(addr, None)
    lat, lng = local_db['coordinates'][uid]
    return uid, lat, lng


def parse_vrp_problem(data):
    real_nodes = []
    problem = list(filter(lambda l: not l.strip().startswith('#'), data.split('\n')))
    depo = list(map(str.strip, problem[0].split(';')))
    real_nodes.append(
        NODE(*([int(item) if item.isdigit() else item for item in depo[:-1]] + [1, 0.0] + list(get_db_info(depo[1])))))
    ants_count = int(depo[-1])
    ants = [ANT(*[int(item) if item.isdigit() else item for item in list(map(str.strip, ant.split(';')))]) for ant in
            problem[1:ants_count]]
    problem = problem[ants_count + 1:]
    for line in problem:
        tmp = list(map(str.strip, line.split(';')))
        real_nodes.append(NODE(*[int(item) if item.isdigit() else item for item in tmp], *list(get_db_info(tmp[1]))))
    return real_nodes, ants


def solve_vrp(nodes, vehicles):
    def real_distance_time(a, b: NODE) -> (float, float):
        if not a.db_uid or not b.db_uid:
            return float('inf'), float('inf')
        else:
            return local_db['matrix'][a.db_uid][b.db_uid]['distance'], local_db['matrix'][a.db_uid][b.db_uid]['time']

    world = pants.World(nodes, real_distance_time)
    solver = pants.Solver(ant_capacity=sum([ant.capacity for ant in vehicles]) / len(vehicles),
                          iterations=100, ant_count=15)

    vrp_solution = []
    unreachable_clients = []
    nodes_left = world.real_nodes()
    total_nodes = len(nodes_left)

    while len(nodes_left) != 1:
        logging.info('%s done' % (100 - (len(nodes_left) / total_nodes)))
        # print(len(world.real_nodes()) - 1)
        solution = solver.solve(world)
        # nodes_left = solution.remaining_moves()
        if len(solution.visited) == 1:  # Nothing was visited
            # TODO: use saved unreachable clients somehow
            unreachable_clients.append(solution.unvisited)
            break
        vrp_solution.append(solution.clone())
        nodes_left = [v for i, v in enumerate(nodes_left) if i not in frozenset(solution.visited[1:])]
        del world
        world = pants.World(nodes_left, real_distance_time)
        # print(solution.distance)

    return vrp_solution, unreachable_clients


def format_solution(solution, unreachable_clients, time):
    total_visited = sum([len(route.visited) - 1 for route in solution])
    return [
        {
            'routes': [[
                           {
                               'address': node.address,
                               'at': route._timeline[i],
                               'demand': node.demand,
                               'lat': node.lat,
                               'lng': node.lng,
                               'st': node.st,
                               'tie': node.tie,
                               'tis': node.tis
                           }
                           for i, node in enumerate(route.tour)]
                       for route in solution],
            'totals': {
                'distance': sum([route.distance for route in solution]),
                'clients_visited': total_visited,
                'clients_total': total_visited + len(unreachable_clients),
                'routes': len(solution),
                'time': "%.1f" % time,
            }
        }
    ]


class HTTPServer_RequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)

        self.send_header('Content-type', 'text/html')
        self.end_headers()

        message = "Hello world!"
        self.wfile.write(bytes(message, "utf8"))
        return

    def do_POST(self):
        length = int(self.headers['Content-Length'])
        post_data = parse.parse_qs(self.rfile.read(length).decode('utf-8')).get('data', '')[0]
        start_time = time.time()
        real_nodes, vehicles = parse_vrp_problem(post_data)
        vrp_solution, unreachable_clients = solve_vrp(real_nodes, vehicles)
        t = time.time() - start_time
        print("--- %s seconds ---" % t)
        formatted_response = format_solution(vrp_solution, unreachable_clients, t)
        with open('solution_{clients}_{total}_{date}.json'.format(
                clients=formatted_response[0]['totals']['clients_total'],
                total=formatted_response[0]['totals']['distance'],
                date=datetime.now().strftime("%Y-%m-%d_%H:%M:%S")),
                'w') as f:
            f.write(json.dumps(formatted_response))

        # formatted_response = json.load(open('solutions/solution_214_2636930_2017-01-14_23:05:16.json'))
        for route in formatted_response[0]['routes']:
            ids = []
            for place in route:
                ids.append(local_db['addresses'].get(place['address'], None))
            print(' '. join(ids[1:]))
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(formatted_response).encode("utf-8"))


def run(test=False):
    if not test:
        print('starting server...')
        server_address = ('127.0.0.1', 8081)
        httpd = HTTPServer(server_address, HTTPServer_RequestHandler)
        print('running server...')
        httpd.serve_forever()
    else:
        from pprint import pprint

        post_data = open('/Users/anatolymaltsev/Downloads/ACO-Pants-0.5.2/text_problem_good.txt').read()
        start_time = time.time()
        real_nodes, vehicles = parse_vrp_problem(post_data)
        vrp_solution, unreachable_clients = solve_vrp(real_nodes, vehicles)
        t = time.time() - start_time
        print("--- %s seconds ---" % t)
        formatted_response = format_solution(vrp_solution, unreachable_clients, t)
        pprint(formatted_response)


def regenerate_solution():
    raw = open('/Users/anatolymaltsev/Downloads/eugen_task.txt').readlines()
    total_distance = 0
    for line in raw:
        route = ['000000000'] + line.split()
        for i, n in enumerate(route):
            j = i % len(route)
            k = (i + 1) % len(route)
            total_distance += local_db['matrix'][route[j]][route[k]]['distance']
    print(total_distance)

# regenerate_solution()
run(test=False)
