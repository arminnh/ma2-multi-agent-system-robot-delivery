import json
from pprint import pprint
import glob
import re

def getAllExperiments():
    experiments = {}
    for file in glob.glob("experiment-*.json"):
        fileName = file.split("/")[-1]
        experimentNr = re.match("experiment-([\d]+).json", fileName).group(1)
        e = json.load(file)
        experiments[experimentNr] = e

    return experiments

if __name__ == "__main__":

    experiments = getAllExperiments()

    pprint(experiment1)
    print("experiment1[experiment]:", experiment1["experiment"])
    print("experiment1[parameters][numRobots]:", experiment1["parameters"]["numRobots"])
    print("experiment1[results][0][totalTasksWaitingTime]:", experiment1["results"][0]["totalTasksWaitingTime"])