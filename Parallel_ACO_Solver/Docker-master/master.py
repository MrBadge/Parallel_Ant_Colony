import json
import logging
import os
import random
import socket
import string
# import hug
import sys
from collections import namedtuple
from threading import Thread

logging.getLogger("falcon").setLevel(logging.WARNING)

input_problem = 'text_problem_good.txt'
NODE = namedtuple('NODE', 'uid,address,demand,tis,tie,st,isReal,becomeRealProbability,db_uid,lat,lng')
ANT = namedtuple('ANT', 'capacity,tis,tie')
local_db = json.load(open('final_matrix.json'))
SLAVES = {}
MAX_BUFFER_SIZE = 1024 * 5000
BIND_PORT = os.getenv('BIND_PORT', 8081)
BIND_HOST = os.getenv('BIND_HOST', '0.0.0.0')
VALID_EVENTS = namedtuple('events', 'NEW_SLAVE,SLAVE_CHECK,NEW_TASK')('new_slave', 'check_slave', 'new_task')


def setup_logger(verbosity_level, name=None):
    root = logging.getLogger()
    root.handlers = []
    if name:
        root.name = name
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


# @hug.post()
# def new_task(raw_task: hug.types.text):
#     """Add new task"""
#
#     def get_solution(host, port, task, index, key):
#         try:
#             s = socket.socket()
#             s.connect((host, port))
#             s.send(task.encode())
#             s.settimeout(50)
#             tmp = s.recv(MAX_BUFFER_SIZE)
#             logging.info('Got raw solution from slave %s: %s' % (key, tmp))
#             results[index] = (key, json.loads(tmp.decode('utf-8')))
#         except Exception as e:
#             logging.error('Exception in thread: %s' % e)
#             results[index] = (k, {})
#         return True
#
#     logging.warning('Master got new task!')
#     logging.warning('Going to create %s threads' % len(SLAVES))
#     if not SLAVES:
#         return hug.HTTP_500
#     threads = []
#     results = [{} for _ in SLAVES.items()]
#     i = 0
#     for k, v in SLAVES.items():
#         process = Thread(target=get_solution, args=[v['host'], v['port'], raw_task, i, k])
#         process.start()
#         threads.append(process)
#         i += 1
#
#     for process in threads:
#         process.join()
#
#     logging.warning('Got %s results' % len(results))
#     # print('Here they are: %s' % results)
#     best_solution = None
#     for k, result in results:
#         if not result:
#             logging.warning('Something wrong with the %s slave, removing it!' % k)
#             SLAVES.pop(k, None)
#             continue
#         logging.warning(result[0]['totals'])
#         res_dist = result[0]['totals']['distance']
#         if not best_solution:
#             best_solution = result
#         else:
#             if res_dist < best_solution[0]['totals']['distance']:
#                 best_solution = result
#     return best_solution
#
#
# @hug.put()
# def new_slave(slave_ip: hug.types.text, slave_port: hug.types.number):
#     """Add new slave"""
#     key = ''.join(random.choices(string.ascii_uppercase + string.digits, k=5))
#     SLAVES[key] = {'host': slave_ip, 'port': slave_port, 'got_solution': False}
#     logging.warning('New slave: %s' % SLAVES[key])
#     return {'key': key}
#
#
# @hug.put()
# def check_slave(slave_ip: hug.types.text, slave_port: hug.types.number, slave_key: hug.types.text):
#     """Add new slave"""
#     if slave_key not in SLAVES:
#         SLAVES[slave_key] = {'host': slave_ip, 'port': slave_port, 'got_solution': False}
#         logging.warning('New slave: %s' % SLAVES)
#     return {'key': slave_key}
#
#
# @hug.get()
# def get_slaves():
#     return SLAVES


def solve_task(raw_task, task_setter_connection=None, async=True):
    def get_solution(conn, task, index, key):
        try:
            conn.sendall(task.encode())
            conn.settimeout(60 * 15)
            tmp = conn.recv(MAX_BUFFER_SIZE).decode('utf-8')
            while tmp[-1] != ']':
                tmp += conn.recv(MAX_BUFFER_SIZE).decode('utf-8')
            logging.debug('Got raw solution from slave %s: %s' % (key, tmp))
            results[index] = (key, json.loads(tmp))
        except Exception as e:
            logging.error('Exception in thread: %s' % e)
            results[index] = (k, {})
        return True

    threads = []
    results = [{} for _ in SLAVES.items()]
    i = 0
    for k, v in SLAVES.items():
        process = Thread(target=get_solution, args=[v['connection'], raw_task, i, k])
        process.start()
        threads.append(process)
        i += 1

    for process in threads:
        process.join()

    logging.warning('Got %s result(s)' % len(results))
    best_solution = None
    for k, result in results:
        if not result:
            logging.warning('Something wrong with the %s slave, removing it!' % k)
            SLAVES.pop(k, None)
            continue
        logging.warning(result[0]['totals'])
        res_dist = result[0]['totals']['distance']
        res_time = result[0]['totals']['time']
        if not best_solution:
            best_solution = result
        else:
            if res_dist < best_solution[0]['totals']['distance']:
                best_solution = result
    if async:
        logging.info('Sending the solution back!')
        task_setter_connection.sendall((json.dumps(best_solution) + '&').encode('utf-8'))
    else:
        return best_solution


def start_server():
    soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    soc.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    logging.info('Rocket launching!')

    try:
        soc.bind((BIND_HOST, BIND_PORT))
        logging.info('Binding on port %s completed' % BIND_PORT)
    except socket.error as msg:
        logging.error('Bind failed. Error : ' + str(sys.exc_info()))
        sys.exit()

    soc.listen(10)
    logging.info('Master is ready!')

    # from threading import Thread

    while True:
        conn, addr = soc.accept()
        ip, port = str(addr[0]), str(addr[1])
        logging.debug('Accepting connection from ' + ip + ':' + port)
        try:
            initial_event = conn.recv(MAX_BUFFER_SIZE).decode('utf-8')
            while initial_event[-1] != '}':
                initial_event += conn.recv(MAX_BUFFER_SIZE).decode('utf-8')
            logging.debug('Master got message: %s' % initial_event)
            initial_event = json.loads(initial_event)
            event = initial_event.get('event', None)
            if not event:
                logging.error('Corrupted data! Event not found')
                continue
            if event == VALID_EVENTS.NEW_SLAVE:
                key = ''.join(random.choices(string.ascii_uppercase + string.digits, k=5))
                SLAVES[key] = {'host': ip, 'port': port, 'connection': conn}
                logging.warning('New slave: %s' % key)
                conn.sendall(json.dumps({'event': event, 'data': {'key': key}}).encode('utf-8'))
            elif event == VALID_EVENTS.NEW_TASK:
                logging.warning('Got new task, preparing guns!')
                raw_task = initial_event.get('data', {}).get('raw_task', '')
                logging.info('Solving...')
                # solution = solve_task(json.dumps({'event': event, 'data': {'raw_task': raw_task}}), conn, async=False)
                # conn.sendall(solution.encode())
                process = Thread(target=solve_task, args=[json.dumps({'event': event, 'data': {'raw_task': raw_task}}),
                                                          conn])
                process.start()
            else:
                logging.error('Unknown event: %s!' % event)
        except:
            logging.error("Terrible error!")
            import traceback
            traceback.print_exc()
    soc.close()


if __name__ == '__main__':
    setup_logger(logging.INFO, name='master')
    start_server()
