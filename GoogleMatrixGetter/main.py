# -*- encoding: utf-8 -*-
import googlemaps
import json
import sys
from os import listdir
from os.path import isfile, join
from time import sleep
import requests
from itertools import islice

g_api_key = open('g_api.key').read()
y_api_key = open('y_api.key').read()


def printProgress(iteration, total, prefix='', suffix='', decimals=1, barLength=100):
    formatStr = "{0:." + str(decimals) + "f}"
    percents = formatStr.format(100 * (iteration / float(total)))
    filledLength = int(round(barLength * iteration / float(total)))
    bar = '█' * filledLength + '-' * (barLength - filledLength)
    sys.stderr.write('\r%s |%s| %s%s %s' % (prefix, bar, percents, '%', suffix)),
    if iteration == total:
        sys.stderr.write('\n')
    sys.stderr.flush()


def chunked(it, size):
    it = iter(it)
    while True:
        p = tuple(islice(it, size))
        if not p:
            break
        yield p

# STEP 1
raw_data = {item.split('\t')[0]: item.split('\t')[1].strip() for item in open('data.txt', 'r').readlines()}
total = len(raw_data)
counter = 0

for _id, addr in raw_data.iteritems():
    printProgress(counter, total, prefix='Progress:', decimals=2, suffix='Complete')
    payload = {'geocode': addr, 'apikey': y_api_key, 'format': 'json'}
    r = requests.get('https://geocode-maps.yandex.ru/1.x/', params=payload)
    if r.status_code == 200:
        pos = json.loads(r.text)
        pos = pos['response']['GeoObjectCollection']['featureMember'][0]['GeoObject']['Point']
        raw_data[_id] = (pos['pos'].split()[1], pos['pos'].split()[0])
    else:
        print 'Yandex API error on addr: ' + addr
    counter += 1

with open('geocoded_data.txt', 'w') as f:
    json.dump(raw_data, f)


# STEP 2
raw_data = json.loads(open('geocoded_data.txt', 'r').read())

chunk_size = 10
counter = 0
total = (len(raw_data) / chunk_size) + 1 if (len(raw_data) > chunk_size) else 1
task_part = 0
for data in chunked(raw_data.iteritems(), chunk_size):
    origins = [x[1] for x in data]
    for jdata in chunked(raw_data.iteritems(), chunk_size):
        destinations = [x[1] for x in jdata]
        with open('input/' + str(task_part) + '.json', 'w') as f:
            json.dump({'data': data, 'jdata': jdata}, f)
        task_part += 1

    counter += 1
    printProgress(counter, total, prefix='Progress:', decimals=2, suffix='Complete')

# STEP 3
tasks = [f for f in listdir('input') if isfile(join('input', f))]
solved = [f for f in listdir('output') if isfile(join('output', f))]
tasks_to_solve = list(set(tasks) - set(solved))

gmaps = googlemaps.Client(key=g_api_key)
total = len(tasks)
solved_count = len(solved)
for task_file in tasks_to_solve:
    printProgress(solved_count, total, prefix='Progress:', decimals=2, suffix='Complete')
    result_matrix = {}
    data = json.load(open('input/' + task_file, 'r'))
    for place in data['data']:
        result_matrix[place[0]] = {}
    origins = [x[1] for x in data['data']]
    destinations = [x[1] for x in data['jdata']]
    google_resp = gmaps.distance_matrix(origins, destinations, mode='driving', units='metric')
    unknown_indexies = []
    for i in xrange(len(google_resp['destination_addresses'])):
        if not google_resp['destination_addresses'][i]:
            print 'Google don\'t know this address: {addr}'.format(addr=destinations[i].decode('utf-8'))
            unknown_indexies.append(i)
    for i in xrange(len(google_resp['origin_addresses'])):
        for j in xrange(len(google_resp['destination_addresses'])):
            if j in unknown_indexies:
                continue
            result_matrix[data['data'][i][0]][data['jdata'][j][0]] = {
                'distance': google_resp['rows'][i]['elements'][j]['distance']['value'],
                'time': google_resp['rows'][i]['elements'][j]['duration']['value']
            }
    save_file = 'output/' + task_file
    f = open(save_file, 'w')
    json.dump(result_matrix, f)
    f.close()
    solved_count += 1


# STEP 4
partial_matrices = [f for f in listdir('output') if isfile(join('output', f))]
total_result = {}
total = len(partial_matrices)
current = 0
for part in partial_matrices:
    try:
        data_part = json.load(open('output/' + part, 'r'))
    except Exception, e:
        print e
        print part
        break
    printProgress(current, total, prefix='Progress:', decimals=2, suffix='Complete')
    for k in data_part.keys():
        tmp = data_part[k].copy()
        if k not in total_result:
            total_result[k] = {}
        total_result[k].update(tmp)
    current += 1
with open('final_matrix.json', 'w') as f:
    json.dump(total_result, f)


# STEP 5
tmp = json.load(open('final_matrix.json', 'r'))
print len(tmp)
