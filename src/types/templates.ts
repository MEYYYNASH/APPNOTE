export interface NoteTemplate {
  id: string;
  title: string;
  description: string;
  category: 'Work' | 'Personal' | 'Study' | 'Meeting' | 'Creative';
  icon: string;
  defaultTitle: string;
  defaultBody: string;
  isChecklist: boolean;
  checklist?: { id: string; text: string; isDone: boolean }[];
  tags: string[];
}

export const NOTE_TEMPLATES: NoteTemplate[] = [
  {
    id: 'meeting-notes',
    title: 'Meeting Notes',
    description: 'Attendees, agenda, key decisions, and action items',
    category: 'Meeting',
    icon: 'Users',
    defaultTitle: 'Team Sync Meeting',
    defaultBody: `## Meeting Overview\n**Date:** ${new Date().toLocaleDateString()}\n**Time:** 10:00 AM\n**Facilitator:** \n**Attendees:** \n\n---\n\n### 🎯 Objectives\n1. \n2. \n\n### 📝 Discussion & Key Points\n- \n- \n\n### ✅ Action Items\n- [ ] Task 1 (Assignee) - Due: \n- [ ] Task 2 (Assignee) - Due: \n\n### 📌 Decisions Made\n- `,
    isChecklist: false,
    tags: ['meeting', 'work'],
  },
  {
    id: 'project-plan',
    title: 'Project Roadmap',
    description: 'Milestones, deliverables, risks, and deadlines',
    category: 'Work',
    icon: 'Briefcase',
    defaultTitle: 'Project Blueprint',
    defaultBody: `## 🚀 Project Overview\n**Project Name:** \n**Lead:** \n**Target Launch:** \n\n### 🎯 Core Objectives\n1. \n2. \n\n### 🗓 Milestones\n| Phase | Target Date | Deliverable | Status |\n| --- | --- | --- | --- |\n| Phase 1: Planning | Wk 1 | Specifications | In Progress |\n| Phase 2: Design | Wk 2 | Wireframes | Pending |\n| Phase 3: Build | Wk 3-4 | MVP App | Pending |\n| Phase 4: Review | Wk 5 | Quality Test | Pending |\n\n### ⚠️ Risks & Mitigations\n- Risk: \n  - Mitigation: `,
    isChecklist: false,
    tags: ['project', 'roadmap'],
  },
  {
    id: 'daily-planner',
    title: 'Daily Checklist & Planner',
    description: 'Priorities, tasks, habits, and daily reflection',
    category: 'Personal',
    icon: 'CheckSquare',
    defaultTitle: `Daily Planner — ${new Date().toLocaleDateString()}`,
    defaultBody: `### 🌟 Top 3 Priorities for Today\n1. \n2. \n3. \n\n### 🧘 Health & Habits\n- [ ] 8 Glasses of Water\n- [ ] 30 min Exercise or Walk\n- [ ] Read 15 pages\n\n### 💭 End of Day Reflection\n- What went well today?\n- What can I improve tomorrow?`,
    isChecklist: true,
    checklist: [
      { id: 'dp1', text: 'Top Priority 1', isDone: false },
      { id: 'dp2', text: 'Top Priority 2', isDone: false },
      { id: 'dp3', text: 'Top Priority 3', isDone: false },
      { id: 'dp4', text: 'Drink 2L of water', isDone: false },
      { id: 'dp5', text: 'Exercise 30 mins', isDone: false },
      { id: 'dp6', text: 'Evening review & winddown', isDone: false },
    ],
    tags: ['daily', 'habits'],
  },
  {
    id: 'cornell-study',
    title: 'Cornell Study Notes',
    description: 'Systematic lecture & book summary framework',
    category: 'Study',
    icon: 'GraduationCap',
    defaultTitle: 'Study Notes: Subject / Topic',
    defaultBody: `## Topic: \n**Source / Chapter:** \n**Date:** ${new Date().toLocaleDateString()}\n\n### 💡 Cue Questions / Keywords\n- Key Term 1: \n- Core Question 2: \n\n### 📖 Lecture Notes & Definitions\n- Definition: \n- Formula / Concept: \n\n> "Important quote or rule from reading"\n\n### 📌 Summary (in your own words)\nIn brief: `,
    isChecklist: false,
    tags: ['study', 'learning'],
  },
  {
    id: 'creative-brainstorm',
    title: 'Brainstorm & Mindmap',
    description: 'Idea generation, pros/cons, and creative sprint',
    category: 'Creative',
    icon: 'Sparkles',
    defaultTitle: 'Idea Sprint: New Concept',
    defaultBody: `## 💡 The Core Problem\nWhat are we solving?\n\n### 🧠 Brainstormed Angles\n- Idea A: \n- Idea B: \n- Idea C: \n\n### ⚖️ Pros vs. Cons Analysis\n| Option | Pros | Cons |\n| --- | --- | --- |\n| Option 1 | Quick to test | Limited scale |\n| Option 2 | High impact | Requires setup |\n\n### 🚀 Immediate Next Experiment\n- Step 1: `,
    isChecklist: false,
    tags: ['brainstorm', 'creative'],
  },
];
