# coding=utf-8
import json

_input = '/Users/anatolymaltsev/Dropbox/MEPhI/УИР/К3М/NIR.txt'


def txt_to_my_format():
    def get_seconds_count(time):
        tmp = time.split(':')
        return int(tmp[0]) * 3600 + int(tmp[1]) * 60 + int(tmp[2])

    with open(_input) as f:
        lines = f.readlines()

    problem = {}
    for line in lines:
        items = line.split('\t')
        try:
            _id = items[0]
            address = items[1]
            packages = int(items[2] if items[2] else 0)
            service_time = get_seconds_count(items[3])
            _from = get_seconds_count(items[4])
            _to = get_seconds_count(items[5])
        except Exception, e:
            print e
            print items
            continue
        if _from >= _to:
            _from = 0
            _to = 24 * 60 * 60 - 1
        if _id in problem:
            problem[_id]['packages'] += packages
            problem[_id]['service_time'] = min(problem[_id]['service_time'], service_time)
            # problem[_id]['from'] = max(problem[_id]['from'], _from)
            # problem[_id]['to'] = min(problem[_id]['to'], _to)
        else:
            problem[_id] = {
                'address': address,
                'packages': packages,
                'service_time': service_time,
                'from': 0, #_from,
                'to': 24 * 60 * 60 - 1 #_to
            }

    with open('problem.json', 'w') as f:
        json.dump(problem, f)

    with open('text_problem.txt', 'w') as f:
        counter = 2
        lines = []
        for k, v in problem.iteritems():
            lines.append('\t;\t'.join(
                map(str, [counter, v['address'], v['packages'], v['from'], v['to'], v['service_time'], 1, 0.0])) + '\n')
            counter += 1
        f.writelines(lines)


def addresses():
    with open(_input) as f:
        lines = f.readlines()

    addr = {}
    for line in lines:
        items = line.split('\t')
        try:
            addr[items[1]] = items[0]
        except Exception, e:
            print e
            print items
            continue

    with open('addrs.json', 'w') as f:
        json.dump(addr, f)


# addresses()
txt_to_my_format()