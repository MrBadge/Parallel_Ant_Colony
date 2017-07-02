import base64
import json
import logging
import os
import socket
# import requests
import sys
import time
from collections import namedtuple

import pants

NODE = namedtuple('NODE', 'uid,address,demand,tis,tie,st,isReal,becomeRealProbability,db_uid,lat,lng')
ANT = namedtuple('ANT', 'capacity,tis,tie')
local_db = json.load(open('final_matrix.json'))

MASTER = os.getenv('ACO_MASTER', 'http://127.0.0.1:8888')
MASTER_HOST = os.getenv('ACO_MASTER_HOST', '0.0.0.0')
MASTER_PORT = os.getenv('ACO_MASTER_PORT', 8081)
BUFFER_SIZE = 1024 * 5000
KEY = None
VALID_EVENTS = namedtuple('events', 'NEW_SLAVE,SLAVE_CHECK,NEW_TASK')('new_slave', 'check_slave', 'new_task')


def setup_logger(verbosity_level, name=None):
    root = logging.getLogger()
    if name:
        root.name = name
    root.handlers = []
    root.setLevel(verbosity_level)

    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(verbosity_level)
    ch.setFormatter(formatter)
    root.addHandler(ch)


def get_db_info(addr):
    uid = local_db['addresses'].get(addr, None)
    lat, lng = local_db['coordinates'][uid]
    return uid, lat, lng


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


def update_progress(progress):
    barLength = 60
    status = ""
    if isinstance(progress, int):
        progress = float(progress)
    if not isinstance(progress, float):
        progress = 0
        status = "error: progress var must be float\r\n"
    if progress < 0:
        progress = 0
        status = "Halt...\r\n"
    if progress >= 1:
        progress = 1
        status = "Done...\r\n"
    block = int(round(barLength * progress))
    text = "\rPercent: [{0}] {1}% {2}".format("#" * block + "-" * (barLength - block), progress * 100, status)
    sys.stdout.write(text)
    sys.stdout.flush()


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
        # update_progress(100 - ((len(nodes_left) - 1) / total_nodes) * 100)
        logging.info('%.2f%s' % (100 - ((len(nodes_left) - 1) / total_nodes) * 100, '%'))
        # print(solution.distance)

    return vrp_solution, unreachable_clients


# def notify_master(host, port):
#     global KEY
#
#     params = {'slave_ip': host, 'slave_port': port}
#     url = '/new_slave'
#     if KEY:
#         params.update(slave_key=KEY)
#         url = '/check_slave'
#     try:
#         resp = requests.put(MASTER + url, params=params)
#         new_key = resp.json()['key']
#         if KEY != new_key:
#             KEY = new_key
#             logging.warning('Key set: %s' % KEY)
#     except Exception as e:
#         logging.error(e)
#         pass


# class Service(socketserver.BaseRequestHandler):
#     def handle(self):
#         logging.info("Client connected with %s" % str(self.client_address))
#         try:
#             data = self.request.recv(BUFFER_SIZE)
#             data = base64.b64decode(data).decode()
#             real_nodes, vehicles = parse_vrp_problem(data)
#             start_time = time.time()
#             vrp_solution, unreachable_clients = solve_vrp(real_nodes, vehicles)
#             formatted_response = format_solution(vrp_solution, unreachable_clients, time.time() - start_time)
#             self.request.send(json.dumps(formatted_response).encode())
#             return
#         except Exception as e:
#             print(e)
#             pass
#         self.request.send("[]")
#         self.request.close()
#
#
# class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
#     pass


# def socket_server():
#     s = socket.socket()
#     s.bind(('', 0))
#     host = '127.0.0.1'  # ipgetter.myip()
#     notify_master(host, s.getsockname()[1])
#     s.listen(1)
#     conn, addr = s.accept()
#     print("Connection from: " + str(addr))
#     while True:
#         data = conn.recv(BUFFER_SIZE)
#         if not data:
#             continue
#         data = base64.b64decode(data).decode()
#         real_nodes, vehicles = parse_vrp_problem(data)
#         start_time = time.time()
#         vrp_solution, unreachable_clients = solve_vrp(real_nodes, vehicles)
#         t = time.time() - start_time
#         formatted_response = format_solution(vrp_solution, unreachable_clients, t)
#         conn.send(json.dumps(formatted_response).encode())
#
#     conn.close()

#
# def get_free_port():
#     s = socket.socket()
#     s.bind(('', 0))
#     port = s.getsockname()[1]
#     s.close()
#     time.sleep(1)
#     return port
#
#
# def master_notifier_manager(host, port):
#     while True:
#         notify_master(host, port)
#         time.sleep(5)


def run():
    global KEY

    soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    soc.connect((MASTER_HOST, int(MASTER_PORT)))

    logging.info('Informing master ...')
    new_slave = json.dumps({'event': 'new_slave'})
    soc.sendall(new_slave.encode("utf8"))

    logging.info('Waiting for the task!')
    while True:
        try:
            result_string = soc.recv(BUFFER_SIZE).decode("utf8")
            if not result_string:
                continue
            while result_string[-1] != '}':
                result_string += soc.recv(BUFFER_SIZE).decode('utf-8')
            logging.debug('Got message: %s' % result_string)
            initial_event = json.loads(result_string)
            event = initial_event.get('event', None)
            if not event:
                logging.error('Invalid message, event not found!')
                continue
            if event == VALID_EVENTS.NEW_SLAVE:
                key = initial_event.get('data', {}).get('key', None)
                if key:
                    KEY = key
                    logging.info('Slave key set: %s' % KEY)
            elif event == VALID_EVENTS.NEW_TASK:
                logging.info('Got new task, hooray!')
                raw_data = initial_event.get('data', {}).get('raw_task', '')
                data = base64.b64decode(raw_data).decode()
                real_nodes, vehicles = parse_vrp_problem(data)
                start_time = time.time()
                vrp_solution, unreachable_clients = solve_vrp(real_nodes, vehicles)
                formatted_response = format_solution(vrp_solution, unreachable_clients, time.time() - start_time)
                logging.debug('THE SOLUTION: %s' % formatted_response)
                logging.warning('Sending the solution back!')
                soc.sendall(json.dumps(formatted_response).encode('utf-8'))
        except Exception as e:
            logging.error('Weird exception happened!!. Here it is: %s' % e)
            soc.sendall(''.encode('utf-8'))
            pass

            # host = '127.0.0.1'
            # port = get_free_port()
            # t = ThreadedTCPServer(('', port), Service)
            #
            # notifier_daemon = Thread(target=master_notifier_manager, args=[host, port])
            # notifier_daemon.setDaemon(True)
            # notifier_daemon.start()
            # notify_master(host, port)

            # t.serve_forever()


if __name__ == '__main__':
    setup_logger(logging.INFO, name='slave')
    run()
