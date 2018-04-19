import copy
import random

class Table:
    def __init__(self):
        pass

    def __str__(self):
        return "Table"

    def __eq__(self, other):
        return isinstance(other, Table)


class Block:
    def __init__(self, name, location=None):
        self.name = name
        self.location = location
        self.has_on_top = None

    def __str__(self):
        loc = self.location.name if isinstance(self.location, Block) else str(self.location)
        return "Block {} on {}".format(self.name, loc)

    def __eq__(self, other):
        return isinstance(other, Block) and self.name == other.name \
            and self.location == other.location and self.has_on_top == other.has_on_top

    def satisfies(self, other):
        if self.name != other.name:
            return False

        if isinstance(self.location, Block) and isinstance(other.location, Block):
            return self.location.name == other.location.name

        return self.location == other.location


class Robot:
    def __init__(self):
        self.holding = None

    def __eq__(self, other):
        return isinstance(other, Robot) and self.holding.name == other.holding.name

    def __str__(self):
        holding_str = self.holding.name if self.holding is not None else "nothing"
        return "Robot holding {}".format(holding_str)

    def is_holding_something(self):
        return self.holding is not None

    def pick_up_block_applicable(self, block):
        return self.holding is None and block.has_on_top is None

    def pick_up_block(self, block):
        self.holding = block
        if isinstance(block.location, Block):
            block.location.has_on_top = None
        block.location = self
        return self

    def put_down_block_applicable(self, location):
        if self.holding is None:
            return False

        if isinstance(location, Table):
            return True

        return self.holding != location and location.has_on_top is None

    def put_down_block(self, location):
        self.holding.location = location
        if isinstance(location, Block):
            location.has_on_top = self.holding
        self.holding = None
        return self


class State:
    def __init__(self, table=None, blocks=None, robot=None):
        self.table = table
        self.blocks = blocks if blocks is not None else {}
        self.robot = robot

    def __str__(self):
        return "Blocks: " + str([str(b) for b in self.blocks.values()])

    def satisfies(self, other):
        return all(self.blocks[name].satisfies(goal_block) for name, goal_block in other.blocks.items())

    def get_applicable_actions(self):
        actions = []

        if not self.robot:
            return actions

        for b in self.blocks.values():
            if self.robot.pick_up_block_applicable(b):
                actions.append(("pick_up_block", [b.name]))

        if self.robot.is_holding_something():
            if self.robot.put_down_block_applicable(self.table):
                actions.append(("put_down_block", ["table"]))

            for b in self.blocks.values():
                if self.robot.put_down_block_applicable(b):
                    actions.append(("put_down_block", [b.name]))

        return actions

    def execute_action(self, action):
        method, parameters = action
        new_state = copy.deepcopy(self)

        if method == "pick_up_block":
            block_name = parameters[0]
            block = new_state.blocks[block_name]
            new_state.robot.pick_up_block(block)
        elif method == "put_down_block":
            if parameters[0] == "table":
                new_state.robot.put_down_block(new_state.table)
            else:
                block_name = parameters[0]
                block = new_state.blocks[block_name]
                new_state.robot.put_down_block(block)
        return new_state

    def execute_plan(self, plan):
        new_state = copy.deepcopy(self)

        if plan is None:
            return new_state

        for action in plan:
            new_state = new_state.execute_action(action)

        return new_state

# create a plan by forward search
def create_plan(state_start, state_goal):
    state = state_start
    plan = []

    while True:
        if state.satisfies(state_goal):
            return plan

        applicable_actions = state.get_applicable_actions()
        if not len(applicable_actions):
            return None

        action = random.choice(applicable_actions)
        state = state.execute_action(action)
        plan.append(action)

        # print("Applicable", applicable_actions)
        # print("Chosen action", action)
        # print("State", state)
        # print()


if __name__ == '__main__':
    # set up start state
    table = Table()
    a = Block('A', location=table)
    b = Block('B', location=a)
    a.has_on_top = b
    c = Block('C', location=b)
    b.has_on_top = c
    blocks = {"A": a, "B": b, "C": c}
    state_start = State(table, blocks, Robot())
    print("Start state", state_start)

    # set up goal state
    goal_c = Block('C', location=table)
    goal_b = Block('B', location=goal_c)
    goal_c.has_on_top = goal_b
    goal_a = Block('A', location=goal_b)
    goal_b.has_on_top = goal_a
    state_goal = State(blocks={"A": goal_a, "B": goal_b, "C": goal_c})
    print("Goal state", state_goal)

    # create a plan and execute it
    plan = create_plan(state_start, state_goal)
    print("Got plan", plan)
    state = state_start.execute_plan(plan)
    print("End state", state)

    # print(robot)
    # print([str(b) for b in blocks])
    # robot.pick_up_block(c).put_down_block(table)
    # print([str(b) for b in blocks])
    # robot.pick_up_block(b).put_down_block(c)
    # print([str(b) for b in blocks])
    # robot.pick_up_block(a).put_down_block(b)
    # print([str(b) for b in blocks])
