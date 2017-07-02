import json
import socket
from base64 import b64encode

import requests
from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponse
from django.shortcuts import render_to_response


@csrf_exempt
def main(request):
    args = {}
    return render_to_response('index.html', args)


@csrf_exempt
def set_task(request):
    if request.method == 'POST':
        data = request.POST.get('data', '')

        soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        soc.connect(('127.0.0.1', 8081))

        task_msg = json.dumps({'event': 'new_task', 'data': {'raw_task': b64encode(data.encode('utf-8'))}})
        soc.sendall(task_msg)

        res = soc.recv(1024 * 5000).decode("utf8")

        if not res:
            return HttpResponse('Error handling response', content_type="text", status=500)

        while res[-1] != '&':
            res += soc.recv(1024 * 5000).decode("utf8")
        return HttpResponse(res[:-1], content_type="application/json")

        # res = requests.post('http://127.0.0.1:8081/new_task', data={'raw_task': b64encode(data.encode('utf-8'))})
        # tmp = json.loads(res.text)
        # if res.status_code == 200 and res.text:
        #     return HttpResponse(res.text, content_type="application/json")
        # else:
        #     return HttpResponse('Error handling response', content_type="text", status=res.status_code)
    else:
        return HttpResponse('Use POST request', content_type="text", status=500)
