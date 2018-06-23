import json
from pprint import pprint

if __name__ == "__main__":

    with open("experiment-1.json") as f:
        experiment1 = json.load(f)

    pprint(experiment1)
    print("experiment1[experiment]:", experiment1["experiment"])
    print("experiment1[parameters][numRobots]:", experiment1["parameters"]["numRobots"])
    print("experiment1[results][0][totalTasksWaitingTime]:", experiment1["results"][0]["totalTasksWaitingTime"])