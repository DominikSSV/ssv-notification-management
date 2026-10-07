const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
async function run(name, plugin, inputs = {}) {
  const params = {...inputs}; let completions = 0;
  await new Promise(resolve => {
    const context = {window: {CapacitorPlugins: plugin ? {SSVNotificationManagement: plugin} : {}},
      $parameters: params, $resolve: () => {completions++; resolve();}};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../odc', name), 'utf8'), context);
  });
  assert.equal(completions, 1); return params;
}
test('missing plugin completes with error instead of blocking startup', async () => {
  const r = await run('GetDisplayedNotifications.js', null);
  assert.equal(r.Success, false); assert.match(r.ErrorMessage, /not installed/);
});
test('native read rejection completes without successful empty list', async () => {
  const r = await run('GetDisplayedNotifications.js', {getDisplayedNotifications: async () => {throw Error('native failure');}});
  assert.equal(r.Success, false); assert.equal(r.ErrorMessage, 'native failure');
});
test('read preserves distinct notifications and null tag', async () => {
  const items = [{id:0,tag:'A',postTime:'100',extras:{TaskId:'4711'}}, {id:0,tag:null,postTime:'101'}];
  const r = await run('GetDisplayedNotifications.js', {getDisplayedNotifications:async()=>({notifications:items})});
  assert.equal(r.Success,true); assert.deepEqual(JSON.parse(r.NotificationsJSON).notifications,items);
});
test('removal passes exactly selected identity including zero id and null tag', async () => {
  let received;
  const r = await run('RemoveDisplayedNotification.js', {removeDisplayedNotification:async x=>{received=x;return {removed:true};}},
    {NotificationJSON:JSON.stringify({id:0,tag:null,postTime:'101',extras:{TaskId:'4711'}})});
  assert.deepEqual(JSON.parse(JSON.stringify(received)), {id:0,tag:null,postTime:'101'});
  assert.equal(r.Removed,true);
});
test('malformed selection never calls native removal', async () => {
  let called = false;
  const r = await run('RemoveDisplayedNotification.js', {removeDisplayedNotification:async()=>{called=true;}},
    {NotificationJSON:JSON.stringify({id:0,postTime:'101'})});
  assert.equal(called,false); assert.equal(r.Success,false);
});
test('already dismissed notification is reported, not assumed removed', async () => {
  const r = await run('RemoveDisplayedNotification.js', {removeDisplayedNotification:async()=>({removed:false})},
    {NotificationJSON:JSON.stringify({id:0,tag:'A',postTime:'100'})});
  assert.equal(r.Success,true); assert.equal(r.Removed,false);
});
